(ns hxegon.homework.person-test
  (:require [clojure.test :refer :all]
            [clojure.string :as string]
            [hxegon.homework.person :as p]))

(def valid-person-args ["last-name" "first-name" "male" "blue" "01/01/2000"])

;; TODO: extract with-tempfile helper macro

(deftest person-test
  (testing "with incorrect number of fields returns error"
    (let [p (p/person (conj valid-person-args "extra arg"))]
      (is (->> p :error-msg nil? not)))
    (let [p (p/person (rest valid-person-args))]
      (is (->> p :error-msg nil? not))))
  (testing "with invalid date returns error"
    (let [args (-> valid-person-args drop-last (conj "0101-2020"))
          p (p/person args)]
      (is (->> p :error-msg nil? not))))
  (testing "with valid arguments returns something with person keys"
    (let [p (p/person valid-person-args)]
      (is (every? #(contains? p %) [:firstname :lastname :gender :fav-color :dob])))))

(deftest people-sorters-test
  (testing "birthdate sorts people by their DOB (ascending)"
    (let [people (map p/person
                      [["Smith" "First" "m" "Blue" "12/25/1999"]
                       ["Smith" "Third" "m" "Blue" "01/02/2001"]
                       ["Smith" "Second" "f" "Blue" "01/01/2001"]
                       ["Smith" "Fourth" "f" "Blue" "01/01/2002"]])
          sorted ((:birthdate p/people-sorters) people)]
      (is (= ["First" "Second" "Third" "Fourth"]
             (map :firstname sorted)))))
  (testing "lastname sorts people by their last name (descending)"
    (let [people (map p/person
                      [["Aaaaa" "First" "m" "Blue" "01/01/2000"]
                       ["Bbbbb" "Third" "m" "Blue" "01/01/2000"]
                       ["Aaaaa" "Second" "f" "Blue" "01/01/2000"]
                       ["Ccccc" "Fourth" "f" "Blue" "01/01/2000"]])
          sorted ((:lastname p/people-sorters) people)]
      (is (= ["Fourth" "Third" "Second" "First"]
             (map :firstname sorted)))))
  (testing "gender sorts female before male, then by last name (ascending)"
    (let [people (map p/person
                      [["Bbbbb" "Fourth" "m" "Blue" "01/01/2000"]
                       ["Aaaaa" "Third" "m" "Blue" "01/01/2000"]
                       ["Aaaaa" "First" "f" "Blue" "01/01/2000"]
                       ["Ccccc" "Second" "f" "Blue" "01/01/2000"]])
          sorted ((:gender p/people-sorters) people)]
      (is (= ["First" "Second" "Third" "Fourth"]
             (map :firstname sorted))))))

(deftest render-dob-test
  (testing "Renders dates as M/D/YYYY"
    (let [date-string "1/1/2001"
          dob (:token (p/parse-dob date-string))
          rendered-dob (p/render-dob dob)]
      (is (= date-string rendered-dob)))))

