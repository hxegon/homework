(ns hxegon.homework.cli-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [clojure.tools.cli :refer [parse-opts]]
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
      (is (= :comma (-> parsed :options :delimiter))))))

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
  (testing "with minimum valid input, no errrors"
    (let [state (cli/args->initial-state ["-f" "deps.edn"])]
      (println "state =" state)
      (is (->> state :options nil? not))
      (is (->> state :exit-message nil?)))))
