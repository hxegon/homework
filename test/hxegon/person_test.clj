(ns hxegon.person-test
  (:require [clojure.test :refer :all]
            [hxegon.person :as p])
  (:import [hxegon.person Person]))

(def valid-person-args ["last-name" "first-name" "male" "blue" "01/01/2000"])

(deftest person-test
  (testing "with incorrect number of fields returns errors"
    (let [p (p/person (conj valid-person-args "extra arg"))]
      (is (->> p :errors empty? not)))
    (let [p (p/person (rest valid-person-args))]
      (is (->> p :errors empty? not))))
  (testing "with invalid date returns errors"
    (let [args (-> valid-person-args drop-last (conj "0101-2020"))
          p (p/person args)]
      (is (->> p :errors empty? not))))
  (testing "with valid arguments returns something with person keys"
    (let [p (p/person valid-person-args)]
      (is (every? #(contains? p %) [:firstname :lastname :gender :fav-color :dob])))))
