(ns hxegon.homework.read-test
  (:require [hxegon.homework.read :as r]
            [hxegon.homework.person :as p]
            [clojure.test :refer [deftest testing is]]
            [clojure.string :as string]))

(deftest rename-person-keys-test
  (testing "Renames keys to expected strings"
    (let [john (p/person ["Smith" "John" "Male" "blue" "01/01/2001"])
          bdate (:dob john)
          readable-john (r/rename-person-keys john)]
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
          return (r/render-parse-error error)]
      (is (->> (vals error)
               (map str)
               (map #(string/includes? return %))
               (every? identity))))))

; Make a temporary file with a 1 valid and 1 invalid person, read and test, close and delete file
(deftest read-people-file-test
  (let [tempfile (java.io.File/createTempFile "read-people-file-test-tempfile" ".txt")
        filepath (.getAbsolutePath tempfile)
        people (->> ["Doe | Jane | Female | Red | 01/02/2001" ; valid person
                     "Smith | John | Male | Blue"]            ; invalid person: wrong number of fields
                    (string/join \newline))]
    (with-open [wrtr (clojure.java.io/writer tempfile)]
      (.write wrtr people))
    (let [results (r/read-people-file :pipe filepath)]
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

(deftest print-people-state-test
  (testing "people data is sent to *out*"
    (let [people-data [["Smith" "John" "Male" "blue" "1/1/2000"]
                       ["Doe" "Jane" "Female" "red" "1/2/2000"]]
          people (map p/person people-data)
          result (with-out-str (r/print-people-state {:people people}))]
      (is (->> (flatten people-data)
               (map #(string/includes? result %))
               (every? identity)))))
  (testing "error data is sent to *out*"
    (let [errors [{:file "foo.txt" :line 1 :msg "barbaz"}]
          error-data (->> errors first vals (map str))
          result (with-out-str (r/print-people-state {:errors errors}))]
      (is (->> error-data
               (map #(string/includes? result %))
               (every? identity)))
      (testing "unless :silent"
        (let [silent-result (with-out-str (r/print-people-state {:errors errors :options {:silent true}}))]
          (println silent-result)
          (is (= '(false false false) (map #(string/includes? silent-result %) error-data))))))))
