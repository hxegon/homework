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
      (is (->> parsed :errors empty? not)))))
