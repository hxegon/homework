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

; Make a temporary file with a 1 valid and 1 invalid person, read and test, close and delete file
(deftest read-people-file-test
  (let [tempfile (java.io.File/createTempFile "read-people-file-test-tempfile" ".txt")
        filepath (.getAbsolutePath tempfile)
        people (->> ["Doe | Jane | Female | Red | 01/02/2001" ; valid person
                     "Smith | John | Male | Blue"]            ; invalid person: wrong number of fields
                    (string/join \newline))]
    (with-open [wrtr (clojure.java.io/writer tempfile)]
      (.write wrtr people))
    (let [results (p/read-people-file :pipe filepath)]
      (testing "correct number of people and errors"
        (is (->> results :people count (= 1)))
        (is (->> results :errors count (= 1))))
      (testing "error on expected line"
        (is (->> results :errors first :line (= 2))))
      (testing "error on expected file"
        (is (->> results :errors first :file (= filepath))))
      (testing "person has expected firstname"
        (is (->> results :people first :firstname (= "Jane")))))
    (.delete tempfile)))

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

(deftest rename-person-keys-test
  (testing "Renames keys to expected strings"
    (let [john (p/person ["Smith" "John" "Male" "blue" "01/01/2001"])
          bdate (:dob john)
          readable-john (p/rename-person-keys john)]
      (is (= ["Smith" "John" "Male" "blue" bdate]
             (map #(get readable-john %)
                  ["last name"
                   "first name"
                   "gender"
                   "favorite color"
                   "birthdate"]))))))

(deftest render-parse-error-test
  (testing "return string includes all error data"
    (let [error {:file "foo.txt" :line 1 :msg "barbaz"}
          return (p/render-parse-error error)]
      (is (->> (vals error)
               (map str)
               (map #(string/includes? return %))
               (every? identity))))))

(deftest render-dob-test
  (testing "Renders dates as M/D/YYYY"
    (let [date-string "1/1/2001"
          dob (:token (p/parse-dob date-string))
          rendered-dob (p/render-dob dob)]
      (is (= date-string rendered-dob)))))

