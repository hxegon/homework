(ns hxegon.homework-test
  (:require [clojure.test :refer :all]
            [hxegon.homework :as hw]
            [clojure.string :as str]
            [clojure.tools.cli :refer [parse-opts]]))

(deftest cli-options-test
  (testing "-f rejects files that don't exist with error"
    (let [args ["-f" "Doesntexist.txt"]
          parsed (parse-opts args hw/cli-options)]
      (is (empty? (get-in parsed [:options :file])))
      (is (->> parsed :errors empty? not))))
  (testing "-f can handle multiple files"
    (let [args ["-f" "deps.edn" "-f" "README.md"]
          parsed (parse-opts args hw/cli-options)]
      (is (= 2 (count (get-in parsed [:options :file]))))))
  (testing "-s errors with invalid method name"
    (let [args ["-s" "foo"]
          parsed (parse-opts args hw/cli-options)]
      (is (->> parsed :errors empty? not))))
  (testing "-d has errors with an unknown or empty DELIM option"
    (let [args ["-d" "tab" "-f" "deps.edn"]
          parsed (parse-opts args hw/cli-options)]
      (is (->> parsed :errors :empty? not)))
    (let [args ["-d" "" "-f" "deps.edn"]
          parsed (parse-opts args hw/cli-options)]
      (is (->> parsed :errors empty? not))))
  (testing "-d has no errors with a valid DELIM option"
    (let [args ["-d" "comma" "-f" "deps.edn"]
          parsed (parse-opts args hw/cli-options)]
      (is (= :comma (-> parsed :options :delimiter))))))

(deftest opts->action-test
  (testing "-h/--help should have non-empty :exit-message and true :ok?"
    (let [opts (parse-opts ["-h"] hw/cli-options)
          action (hw/opts->action opts)]
      (is (:ok? action))
      (is (->> action :exit-message empty? not))))
  (testing "with errors in opts, :ok? false and non-empty :exit-message"
    (let [opts (parse-opts ["-f" "non-existant.txt"] hw/cli-options)
          action (hw/opts->action opts)]
      (is (->> action :ok? not))
      (is (->> action :exit-message empty? not))))
  (testing "with no files specified, non-empty :exit-message and falsey :ok?"
    (let [opts (parse-opts [] hw/cli-options)
          action (hw/opts->action opts)]
      (is (->> action :ok? not))
      (is (->> action :exit-message empty? not))))
  (testing "with valid input, no errrors"
    (let [opts (parse-opts ["-f" "deps.edn"] hw/cli-options)
          action (hw/opts->action opts)]
      (println "action =" action)
      (is (->> action :options nil? not))
      (is (->> action :exit-message nil?)))))
