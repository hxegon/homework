(ns hxegon.homework.cli-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [clojure.string :as string]
    [clojure.tools.cli :refer [parse-opts]]
    [hxegon.homework.person :as p]
    [hxegon.homework.cli :as cli]))

(deftest options-test
  (testing "-f rejects files that don't exist with error"
    (let [args ["-f" "Doesntexist.txt"]
          parsed (parse-opts args cli/options)]
      (is (empty? (get-in parsed [:options :file])))
      (is (->> parsed :errors empty? not))))
  (testing "-f can handle multiple files"
    (let [args ["-f" "deps.edn" "-f" "README.md"]
          parsed (parse-opts args cli/options)]
      (is (= 2 (count (get-in parsed [:options :file]))))))
  (testing "-s has errors with invalid method name"
    (let [args ["-s" "foo" "-f" "deps.edn"]
          parsed (parse-opts args cli/options)]
      (is (->> parsed :errors empty? not))))
  (testing "-s has no errors with valid SORT option"
    (let [args ["-s" "gender" "-f" "deps.edn"]
          parsed (parse-opts args cli/options)]
      (is (= :gender (-> parsed :options :sort)))))
  (testing "-d has errors with an unknown or empty DELIM option"
    (let [args ["-d" "tab" "-f" "deps.edn"]
          parsed (parse-opts args cli/options)]
      (is (->> parsed :errors :empty? not)))
    (let [args ["-d" "" "-f" "deps.edn"]
          parsed (parse-opts args cli/options)]
      (is (->> parsed :errors empty? not))))
  (testing "-d has no errors with a valid DELIM option"
    (let [args ["-d" "comma" "-f" "deps.edn"]
          parsed (parse-opts args cli/options)]
      (is (= :comma (-> parsed :options :delimiter)))))
  (testing "-S has true :silent"
    (let [parsed (parse-opts ["-S"] cli/options)]
      (is (->> parsed :options :silent)))))

(deftest args->initial-state-test
  (testing "-h/--help should have non-empty :exit-message and true :ok?"
    (let [state (cli/args->initial-state ["-h"])]
      (is (:ok? state))
      (is (->> state :exit-message empty? not))))
  (testing "with errors in opts, :ok? false and non-empty :exit-message"
    (let [state (cli/args->initial-state ["-f" "non-existant.txt"])]
      (is (->> state :ok? not))
      (is (->> state :exit-message empty? not))))
  (testing "with no files specified, non-empty :exit-message and falsey :ok?"
    (let [state (cli/args->initial-state [])]
      (is (->> state :ok? not))
      (is (->> state :exit-message empty? not))))
  (testing "with a valid input, no errrors"
    (let [state (cli/args->initial-state ["read" "-f" "deps.edn"])]
      (is (->> state :options nil? not))
      (is (->> state :exit-message nil?))))
  (testing "has an :action of :read with a 'read' argument"
    (let [state (cli/args->initial-state ["read" "-f" "deps.edn"])]
      (is (= :read (:action state)))))
  (testing "with invalid action, :ok? false and :exit-message mentioning bad action"
    (let [state (cli/args->initial-state ["wrong"])]
      (is (not (:ok? state)))
      (is (string/includes? (get state :exit-message "no :exit-message") "wrong")))))

(deftest print-people-state-test
  (testing "people data is sent to *out*"
    (let [people-data [["Smith" "John" "Male" "blue" "1/1/2000"]
                       ["Doe" "Jane" "Female" "red" "1/2/2000"]]
          people (map p/person people-data)
          result (with-out-str (cli/print-people-state {:people people}))]
      (println result)
      (is (->> (flatten people-data)
               (map #(string/includes? result %))
               (every? identity)))))
  (testing "error data is sent to *out*"
    (let [errors [{:file "foo.txt" :line 1 :msg "barbaz"}]
          error-data (->> errors first vals (map str))
          result (with-out-str (cli/print-people-state {:errors errors}))]
      (println result)
      (is (->> error-data
               (map #(string/includes? result %))
               (every? identity)))
      (testing "unless :silent"
        (let [silent-result (with-out-str (cli/print-people-state {:errors errors :options {:silent true}}))]
          (println silent-result)
          (is (= '(false false false) (map #(string/includes? silent-result %) error-data))))))))
