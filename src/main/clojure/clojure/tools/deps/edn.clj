;   Copyright (c) Rich Hickey. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns clojure.tools.deps.edn
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as jio]
    [clojure.string :as str]
    [clojure.tools.deps.specs :as specs]
    [clojure.walk :as walk])
  (:import
    [java.io File PushbackReader]
    [clojure.lang EdnReader$ReaderException]
    ))

(set! *warn-on-reflection* true)

;;;; Read

(defonce ^:private nl (System/getProperty "line.separator"))

(defn- printerrln
  "println to *err*"
  [& msgs]
  (binding [*out* *err*
            *print-readably* nil]
    (pr (str (str/join " " msgs) nl))
    (flush)))

(defn- io-err
  "Helper function to construct an ex-info for an exception reading
  file at path with the message format fmt (which should have one
  variable for the path)."
  ^Throwable [fmt & {:keys [path]}]
  (let [abs-path (.getAbsolutePath (jio/file path))]
    (ex-info (format fmt abs-path) {:path abs-path})))

(defn read-edn
  "Read edn from file f, which should contain exactly one edn value.
  If f exists but is blank, nil is returned.
  Throws if file is unreadable or contains multiple values.
  Opts:
    :path String path to file being read"
  [^File f & opts]
  (with-open [rdr (PushbackReader. (jio/reader f))]
    (let [EOF (Object.)
          val (try
                (let [val (edn/read {:default tagged-literal :eof EOF} rdr)]
                  (if (identical? EOF val)
                    nil ;; empty file
                    (if (not (identical? EOF (edn/read {:eof EOF} rdr)))
                      (throw (ex-info "Expected edn to contain a single value." {}))
                      val)))
                (catch EdnReader$ReaderException e
                  (throw (io-err (str (.getMessage e) " (%s)") opts)))
                (catch RuntimeException t
                  (if (str/starts-with? (.getMessage t) "EOF while reading")
                    (throw (io-err "Error reading edn, delimiter unmatched (%s)" opts))
                    (throw (io-err (str "Error reading edn. " (.getMessage t) " (%s)") opts)))))])))

(defn validate
  "Validate a deps-edn map according to the specs, throw if invalid.
  Opts:
    :path String path to file being read"
  [deps-edn & opts]
  (if (specs/valid-deps? deps-edn)
    deps-edn
    (throw (io-err (str "Error reading deps %s. " (specs/explain-deps deps-edn)) opts))))

;;;; Canonicalize

(defn- canonicalize-sym
  [s & opts]
  (if (simple-symbol? s)
    (let [cs (as-> (name s) n (symbol n n))]
      (printerrln "DEPRECATED: Libs must be qualified, change" s "=>" cs
        (if-let [path (:path opts)] (str "(" path ")" "")))
      cs)
    s))

(defn- canonicalize-exclusions
  [{:keys [exclusions] :as coord} & opts]
  (if (seq (filter simple-symbol? exclusions))
    (assoc coord :exclusions (mapv #(canonicalize-sym % opts) exclusions))
    coord))

(defn- canonicalize-dep-map
  [deps-map & opts]
  (when deps-map
    (reduce-kv (fn [acc lib coord]
                 (let [new-lib (if (simple-symbol? lib) (canonicalize-sym lib opts) lib)
                       new-coord (canonicalize-exclusions coord opts)]
                   (assoc acc new-lib new-coord)))
      {} deps-map)))

(defn canonicalize
  "Canonicalize a deps.edn map (convert simple lib symbols to qualified lib symbols).
  Opts:
    :path String path to file being read"
  [deps-edn & opts]
  (walk/postwalk
    (fn [x]
      (if (map? x)
        (reduce (fn [xr k]
                  (if-let [xm (get xr k)]
                    (assoc xr k (canonicalize-dep-map xm opts))
                    xr))
          x #{:deps :default-deps :override-deps :extra-deps :classpath-overrides})
        x))
    deps-edn))

;;;; Read deps with validation and canonicalization

(defn read-deps
  "Corece f to a file with jio/file, then read, validate, and canonicalize
  the deps.edn. This is the primary entry point for reading.

  Opts: none"
  [f & opts]
  (let [file (jio/file f)
        opts {:path (.getPath file)}]
    (when (.exists file)
      (-> file (read-edn opts) (validate opts) (canonicalize opts)))))

;;;; deps edn manipulation

(defn- merge-or-replace
  "If maps, merge, otherwise replace"
  [& vals]
  (when (some identity vals)
    (reduce (fn [ret val]
              (if (and (map? ret) (map? val))
                (merge ret val)
                (or val ret)))
      nil vals)))

(defn merge-edns
  "Merge multiple deps edn maps from left to right into a single deps edn map."
  [deps-edn-maps]
  (apply merge-with merge-or-replace (remove nil? deps-edn-maps)))

;;;; Aliases

;; per-key binary merge-with rules

(def ^:private last-wins (comp last #(remove nil? %) vector))
(def ^:private append (comp vec concat))
(def ^:private append-unique (comp vec distinct concat))

(def ^:private merge-alias-rules
  {:deps merge ;; FUTURE: remove
   :replace-deps merge ;; formerly :deps
   :extra-deps merge
   :override-deps merge
   :default-deps merge
   :classpath-overrides merge
   :paths append-unique ;; FUTURE: remove
   :replace-paths append-unique ;; formerly :paths
   :extra-paths append-unique
   :jvm-opts append
   :main-opts last-wins
   :exec-fn last-wins
   :exec-args merge-or-replace
   :ns-aliases merge
   :ns-default last-wins})

(defn- choose-rule [alias-key val]
  (or (merge-alias-rules alias-key)
    (if (map? val)
      merge
      (fn [_v1 v2] v2))))

(defn- merge-alias-maps
  "Like merge-with, but using custom per-alias-key merge function"
  [& ms]
  (reduce
    #(reduce
       (fn [m [k v]] (update m k (choose-rule k v) v))
       %1 %2)
    {} ms))

(defn combine-aliases
  "Find, read, and combine alias maps identified by alias keywords from
  a deps edn map into a single args map."
  [edn-map alias-kws]
  (->> alias-kws
    (map #(get-in edn-map [:aliases %]))
    (apply merge-alias-maps)))




(defn- chase-key
  "Given an aliases set and a keyword k, return a flattened vector of path
  entries for that k, resolving recursively if needed, or nil."
  [aliases k]
  (let [path-coll (get aliases k)]
    (when (seq path-coll)
      (into [] (mapcat #(if (string? %) [[% {:path-key k}]] (chase-key aliases %))) path-coll))))

(defn- flatten-paths
  [{:keys [paths aliases] :as deps-edn-map} {:keys [extra-paths] :as classpath-args}]
  (let [aliases' (assoc aliases :paths paths :extra-paths extra-paths)]
    (into [] (comp (mapcat #(chase-key aliases' %)) (remove nil?)) [:extra-paths :paths])))

