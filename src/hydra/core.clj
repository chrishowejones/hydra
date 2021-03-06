(ns hydra.core
  (:require [clojure.set :refer [union]]
            [clojure.string :refer [split]]))

(defrecord IndexWrapper [index])

(defn index-wrapper? [x]
  (instance? hydra.core.IndexWrapper x))

(defn- prepend [elem mp]
  (into {} (for [[k v] mp] [(conj k elem) v])))

(defn keys-to-vecs [mp]
  (into {} (for [[k v] mp] [(vec k) v])))

(defn keys-to-path-seq [coll]
  (cond (map? coll) (apply merge (for [[k v] coll] (prepend k (keys-to-path-seq v))))
        (vector? coll) (apply merge (for [i (range (count coll))] (prepend i (keys-to-path-seq (nth coll i)))))
        :else {'() coll}))

;; To and from path map

(defn to-path-map [coll]
  (-> coll keys-to-path-seq keys-to-vecs))

(defn- add-paths [mp [k v]]
  (if (= 1 (count k))
    (assoc mp (first k) v)
    (assoc mp (first k) (add-paths (get mp (first k) {}) [(rest k) v]))))

(defn- from-path-set-to-map-of-maps [path-set]
  (reduce add-paths {} path-set))

(defn- vectorize [m-of-m]
  (let [sub-maps (into {} (for [[k v] m-of-m] [k (if (map? v) (vectorize v) v)]))]
    (if (->> sub-maps keys (every? number?))
      (mapv second (sort-by first (into [] sub-maps)))
      sub-maps)))

(defn from-path-map [pm]
  (-> pm from-path-set-to-map-of-maps vectorize))

;; Basic operators (excluding merge and get)

(defn path [v]
  {(-> v butlast vec) (last v)})

(defn cleave [pm pred]
  (loop [results '(() ()) paths (seq pm)]
    (if (not paths)
      (list (into {} (first results)) (into {} (second results)))
      (let [[k v] (first paths)
            passes (first results)
            fails (second results)]
        (recur (if (pred (conj k v)) (list (conj passes [k v]) fails) (list passes (conj fails [k v]))) (next paths))))))

(defn cross-product [f pm0 pm1]
  (into {} (for [x pm0 y pm1] (f x y))))

;; Some predicates for cleave

(defn starts-with? [route path]
  (when (<= (count route) (count path))
    (every? identity (map #(if (clojure.test/function? %1) (%1 %2) (= %1 %2)) route path))))

(defn ends-with? [route path]
  (starts-with? (reverse route) (reverse path)))

;; Leaf operators

(defn transform-leaf [pm route f] ;<- Wrong
  (let [[passed failed] (cleave pm #(starts-with? route %))
        transformed-passed (into {} (for [[k v] passed] [k (f v)]))]
    (merge transformed-passed failed)))

(def transform-leaves transform-leaf)                       ;; Synonym

(defn reset-leaf [pm route val]
  (transform-leaf pm route (constantly val)))

(def reset-leaves reset-leaf)                               ;; Synonym

(defn- prepend-path [[k0 v0] [k1 v1]]
  [(vec (concat (conj k0 v0) k1)), v1])

(defn upsert [routes-path-set target-path-set inserted-path-set]
  (merge (cross-product prepend-path routes-path-set inserted-path-set) target-path-set))

(defn- width-at [pm route]
  (let [route-len (count route)
        [passed failed] (cleave pm #(starts-with? route %))
        passed-keys (keys passed)
        indexes (map #(nth % route-len) passed-keys)]
    (inc (apply max indexes))))

(defn insert-at [])

(defn insert-before [])

(defn upsert-after [pm route inserted-pm]
  (let [width (width-at pm route)
        new-path (conj route width)]
    (upsert (path new-path) pm inserted-pm)))
