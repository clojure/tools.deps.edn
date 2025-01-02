;   Copyright (c) Rich Hickey. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns clojure.tools.deps.edn
  (:require
    [clojure.java.io :as jio]
    [clojure.string :as str]
    [clojure.tools.deps.util.io :as io]
    [clojure.tools.deps.specs :as specs]
    [clojure.walk :as walk])
  (:import
    [java.io File]
    [clojure.lang EdnReader$ReaderException]
    ))

(set! *warn-on-reflection* true)

;;;; deps.edn reading

(defn- io-err
  ^Throwable [fmt ^File f]
  (let [path (.getAbsolutePath f)]
    (ex-info (format fmt path) {:path path})))

(defn- slurp-edn-map
  "Read the file specified by the path-segments, slurp it, and read it as edn."
  [^File f]
  (let [val (try (io/slurp-edn f)
                 (catch EdnReader$ReaderException e (throw (io-err (str (.getMessage e) " (%s)") f)))
                 (catch RuntimeException t
                   (if (str/starts-with? (.getMessage t) "EOF while reading")
                     (throw (io-err "Error reading edn, delimiter unmatched (%s)" f))
                     (throw (io-err (str "Error reading edn. " (.getMessage t) " (%s)") f)))))]
    (if (specs/valid-deps? val)
      val
      (throw (io-err (str "Error reading deps %s. " (specs/explain-deps val)) f)))))

;; all this canonicalization is deprecated and will eventually be removed

(defn- canonicalize-sym
  ([s]
   (canonicalize-sym s nil))
  ([s file-name]
   (if (simple-symbol? s)
     (let [cs (as-> (name s) n (symbol n n))]
       (io/printerrln "DEPRECATED: Libs must be qualified, change" s "=>" cs
         (if file-name (str "(" file-name ")") ""))
       cs)
     s)))

(defn- canonicalize-exclusions
  [{:keys [exclusions] :as coord} file-name]
  (if (seq (filter simple-symbol? exclusions))
    (assoc coord :exclusions (mapv #(canonicalize-sym % file-name) exclusions))
    coord))

(defn- canonicalize-dep-map
  [deps-map file-name]
  (when deps-map
    (reduce-kv (fn [acc lib coord]
                 (let [new-lib (if (simple-symbol? lib) (canonicalize-sym lib file-name) lib)
                       new-coord (canonicalize-exclusions coord file-name)]
                   (assoc acc new-lib new-coord)))
      {} deps-map)))

(defn- canonicalize-all-syms
  ([deps-edn]
   (canonicalize-all-syms deps-edn nil))
  ([deps-edn file-name]
   (walk/postwalk
     (fn [x]
       (if (map? x)
         (reduce (fn [xr k]
                   (if-let [xm (get xr k)]
                     (assoc xr k (canonicalize-dep-map xm file-name))
                     xr))
           x #{:deps :default-deps :override-deps :extra-deps :classpath-overrides})
         x))
     deps-edn)))

(defn slurp-deps
  "Read a single deps.edn file from disk and canonicalize symbols,
  return a deps map. If the file doesn't exist, returns nil."
  [^File dep-file]
  (when (.exists dep-file)
    (-> dep-file slurp-edn-map (canonicalize-all-syms (.getPath dep-file)))))

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

