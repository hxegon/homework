(ns hxegon.person-test
  (:require [clojure.test :refer :all]
            [clojure.string :as string]
            [hxegon.person :as p]
            [hxegon.homework :refer [delimiters]]))

(def valid-person-args ["last-name" "first-name" "male" "blue" "01/01/2000"])

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
    (let [results (p/read-people-file (:pipe delimiters) filepath)]
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

(deftest read-people-files-test
  (let [tempfile-1 (java.io.File/createTempFile "read-people-files-test-tempfile-1" ".txt")
        filepath-1 (.getAbsolutePath tempfile-1)
        person-1 "Doe | Jane | Female | Red | 01/02/2001"
        tempfile-2 (java.io.File/createTempFile "read-people-files-test-tempfile-2" ".txt")
        filepath-2 (.getAbsolutePath tempfile-2)
        person-2 "Smith | John | Male | Blue | 01/03/2001"]
    (with-open [wrtr (clojure.java.io/writer tempfile-1)]
      (.write wrtr person-1))
    (with-open [wrtr (clojure.java.io/writer tempfile-2)]
      (.write wrtr person-2))
    (let [results (p/read-people-files (:pipe delimiters) [filepath-1 filepath-2])]
      (testing "both people in results with no errors"
        (is (= [] (:errors results)))
        (is (->> results :people first :firstname (= "Jane")))
        (is (->> results :people second :firstname (= "John")))))
    (.delete tempfile-1)
    (.delete tempfile-2)))
