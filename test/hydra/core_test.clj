(ns hydra.core-test
  (:require [midje.sweet :refer :all]
            [midje.util :refer [testable-privates]]
            [hydra.core :refer :all]))

(testable-privates hydra.core from-path-set-to-map-of-maps vectorize)

(def simple-map
  {"a" 1 "b" 2 "c" 3})

(def simple-vec
  ["a" "b" "c"])

(def two-level-map
  {"a" 1 "b" 2 "c" {"d" 3}})

(def deeply-nested-map
  {"a" {"b" 1}
   "c" 2
   "d" {"e" 3
        "f" {"g" 4
             "h" 5}}})

(def simple-map-with-vector
  {"a" ["b" "d" "e"]
   "f" 2})

(def vector-with-map
  [{"a" 1} {"b" 2} {"c" 3}])

(def numeric-keyed-map
  {0 "a"
   1 "b"
   2 "c"})

(def map-with-numeric-keys-representing-vector
  {"a" {0 "b"
        1 "c"
        2 "d"}
   "e" 2})

(def deeply-nested-map-with-vectors
  {"a" {"b" 1}
   "c" ["i" "j" "k"]
   "d" {"e" 3
        "f" {"g" 4
             "h" [5 6 7]}}})


(fact "simple map path-set test"
      (to-path-set simple-map) => #{["a" 1] ["b" 2] ["c" 3]})

(fact "simple vec path-set test"
      (to-path-set simple-vec) => #{[0 "a"] [1 "b"] [2 "c"]})

(fact "two level map test"
      (to-path-set two-level-map) => #{["a" 1] ["b" 2] ["c" "d" 3]})

(fact "A deeply nested map of maps can create a pathmap"
      (to-path-set deeply-nested-map) => #{["a" "b" 1] ["c" 2] ["d" "e" 3] ["d" "f" "g" 4] ["d" "f" "h" 5]})

(fact "You can mix maps and vectors"
      (to-path-set simple-map-with-vector) => #{["a" 0 "b"] ["a" 1 "d"] ["a" 2 "e"] ["f" 2]})

(fact "vectors can be the outermost structure"
      (to-path-set vector-with-map) => #{[0 "a" 1] [1 "b" 2] [2 "c" 3]})

(fact "You can recreate a simple map"
      (from-path-set-to-map-of-maps #{["a" 1] ["b" 2] ["c" 3]}) => simple-map)

(fact "You can recreate a two level map"
      (from-path-set-to-map-of-maps #{["a" 1] ["b" 2] ["c" "d" 3]}) => two-level-map)

(fact "you can recreate a deeply nested map"
      (from-path-set-to-map-of-maps #{["a" "b" 1] ["c" 2] ["d" "e" 3] ["d" "f" "g" 4] ["d" "f" "h" 5]}) => deeply-nested-map)

(fact "vectorizing maps without numeric keys returns an unchanged map"
      (vectorize simple-map) => simple-map
      (vectorize two-level-map) => two-level-map
      (vectorize deeply-nested-map) => deeply-nested-map)

(fact "vectorizing a map with exclusively numeric keys returns a vector"
      (vectorize numeric-keyed-map) => ["a" "b" "c"])

(fact "vectorizing a map with embeded numeric keys returns a map with an embeded vector"
      (vectorize map-with-numeric-keys-representing-vector) => {"a" ["b" "c" "d"] "e" 2})

(fact "you can convert a path set into map of maps and vecs"
      (from-path-set #{["a" 0 "b"] ["a" 1 "d"] ["a" 2 "e"] ["f" 2]}) => simple-map-with-vector)

(fact "you can make a round trip from maps to path sets and back"
      (-> deeply-nested-map-with-vectors to-path-set from-path-set) => deeply-nested-map-with-vectors)

(defn path-longer-than? [len path]
  (> (count path) len))

(fact "You can cleave a set of paths in two with a predicate"
      (-> (cleave (partial path-longer-than? 3) (to-path-set deeply-nested-map-with-vectors)) first) =>
      #{["d" "f" "h" 2 7] ["d" "f" "h" 1 6] ["d" "f" "g" 4] ["d" "f" "h" 0 5]}
      (-> (cleave (partial path-longer-than? 3) (to-path-set deeply-nested-map-with-vectors)) second) =>
      #{["a" "b" 1] ["c" 2 "k"] ["c" 0 "i"] ["d" "e" 3] ["c" 1 "j"]})

(fact "Splicing is the inverse of cleaving"
      (->> deeply-nested-map-with-vectors to-path-set (cleave (partial path-longer-than? 3)) splice from-path-set) => deeply-nested-map-with-vectors)

(fact "modifying leaves"
      (-> deeply-nested-map-with-vectors to-path-set (reset-leaf ["a"] 42) from-path-set) =>
      {"a" {"b" 42}
       "c" ["i" "j" "k"]
       "d" {"e" 3
            "f" {"g" 4
                 "h" [5 6 7]}}})



