(ns clojure.tools.deps.edn.config-test
  (:require
    [clojure.java.io :as jio]
    [clojure.test :refer [deftest is]]
    [clojure.tools.deps.edn.config :as sut]
    [clojure.tools.deps.util.dir :as dir])
  (:import
    [java.io File]
    [java.nio.file Files]
    [java.nio.file.attribute FileAttribute]))

(def ^:private base (.getCanonicalFile (File. ".")))

(deftest test-config-dir-builds-expected-path
  (let [actual (dir/with-dir base (sut/config-dir :project 'org.clojure/tools.repl))
        expected (File. base ".cli-config/org.clojure/tools.repl")]
    (is (= expected actual))))

(deftest test-config-file-builds-expected-path
  (let [actual (dir/with-dir base (sut/config-file :project 'org.clojure/tools.repl "config.clj"))
        expected (File. base ".cli-config/org.clojure/tools.repl/config.clj")]
    (is (= expected actual))))

(deftest test-config-edn-file-builds-expected-path
  (let [actual (dir/with-dir base (sut/config-edn-file :project 'org.clojure/tools.repl))
        expected (File. base ".cli-config/org.clojure/tools.repl.edn")]
    (is (= expected actual))))

(deftest test-config-edn-returns-nil-for-missing-file
  (let [actual (dir/with-dir base (sut/config-edn :project 'org.clojure/tools.repl))]
    (is (nil? actual))))

(deftest test-config-no-args-returns-nil
  (dir/with-dir base (is (nil? (sut/config 'org.clojure/tools.repl)))))

(deftest test-config-merges-defaults-and-overrides
  (let [actual (dir/with-dir base (sut/config 'org.clojure/tools.repl
                                              :defaults {:a 1 :b 2}
                                              :overrides {:a 2}))
        expected {:a 2 :b 2}]
    (is (= expected actual))))

(deftest test-write-config-round-trip
  (let [tmp (.toFile (Files/createTempDirectory "cli-config-test" (into-array FileAttribute [])))
        expected {:a 1 :b "two"}
        actual (dir/with-dir tmp
                 (sut/write-config :project 'org.clojure/tools.repl expected)
                 (sut/config-edn :project 'org.clojure/tools.repl))]
    (is (= expected actual))))

(deftest test-write-val-creates-file-when-missing
  (let [tmp (.toFile (Files/createTempDirectory "cli-config-test" (into-array FileAttribute [])))
        actual (dir/with-dir tmp
                 (sut/write-val :project 'org.clojure/tools.repl :a 1)
                 (sut/config-edn :project 'org.clojure/tools.repl))
        expected {:a 1}]
    (is (= expected actual))))

(deftest test-write-val-preserves-formatting
  (let [tmp (.toFile (Files/createTempDirectory "cli-config-test" (into-array FileAttribute [])))
        file (dir/with-dir tmp (sut/config-edn-file :project 'org.clojure/tools.repl))
        original ";; highly customized\n{:a 1\n ;; the custom b setting\n :b 2}\n"
        expected ";; highly customized\n{:a 42\n ;; the custom b setting\n :b 2}\n"]
    (jio/make-parents file)
    (spit file original)
    (dir/with-dir tmp (sut/write-val :project 'org.clojure/tools.repl :a 42))
    (is (= expected (slurp file)))))

(deftest test-write-val-adds-new-key
  (let [tmp (.toFile (Files/createTempDirectory "cli-config-test" (into-array FileAttribute [])))
        file (dir/with-dir tmp (sut/config-edn-file :project 'org.clojure/tools.repl))
        expected {:a 1 :b 2}]
    (jio/make-parents file)
    (spit file "{:a 1}")
    (dir/with-dir tmp (sut/write-val :project 'org.clojure/tools.repl :b 2))
    (is (= expected (dir/with-dir tmp (sut/config-edn :project 'org.clojure/tools.repl))))))

(deftest test-validate-lib-rejects-unqualified-symbol
  (let [expected-msg #"^lib must be a qualified symbol \(group/artifact\)$"]
    (is (thrown-with-msg? clojure.lang.ExceptionInfo expected-msg (#'sut/validate-lib 'tools.repl)))))

(deftest test-config-dir-rejects-unknown-location
  (let [expected-msg #"^location must be :user or :project$"]
    (is (thrown-with-msg? clojure.lang.ExceptionInfo expected-msg (sut/config-dir :do-what-now 'org.clojure/tool.repl)))))
