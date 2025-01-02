(ns clojure.tools.deps.util)

(defn submap?
  "Is m1 a subset of m2?"
  [m1 m2]
  (if (and (map? m1) (map? m2))
    (every? (fn [[k v]] (and (contains? m2 k)
                          (submap? v (get m2 k))))
      m1)
    (= m1 m2)))

(defn submap-debug?
  "Is m1 a subset of m2?
   Print missing keys or mismatched values."
  [m1 m2]
  (if (and (map? m1) (map? m2))
    (every? (fn [[k v]]
              (when (not (contains? m2 k)) (println "m1 has key, m2 does not: " k))
              (and (contains? m2 k) (submap? v (get m2 k))))
            m1)
    (if (= m1 m2)
      true
      (do
        (println "Nested values don't match, m1 val=" m1 "m2 val=" m2)
        false))))
