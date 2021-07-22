(ns hxegon.homework.cli-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [clojure.string :as string]
    [clojure.tools.cli :refer [parse-opts]]
    [hxegon.homework.person :as p]
    [hxegon.homework.cli :as cli]))

(defn- parse-options [args]
  (parse-opts args cli/options))

(deftest options-test
  (testing "-f"
    (testing "rejects files that don't exist with error"
      (let [parsed (parse-options ["-f" "Doesntexist.txt"])]
        (is (empty? (get-in parsed [:options :file])))
        (is (->> parsed :errors empty? not))))
    (testing "can handle multiple files"
      (let [parsed (parse-options ["-f" "deps.edn" "-f" "README.md"])]
        (is (= 2 (count (get-in parsed [:options :file])))))))
  (testing "-F"
    (testing "with valid input doesn't cause error"
      (let [parsed (parse-options ["-F" "pipe:deps.edn"])]
        (is (->> parsed :errors empty?))))
    (testing "rejects input lacking a colon"
      (let [parsed (parse-options ["-F" "pipedeps.edn"])]
        (is (->> parsed :errors empty? not))))
    (testing "rejects non-existant files"
      (let [parsed (parse-options ["-F" "pipe:non-existant.txt"])]
        (is (->> parsed :errors empty? not))))
    (testing "rejects invalid delimiters"
      (let [parsed (parse-options ["-F" "foo:deps.edn"])]
        (is (->> parsed :errors empty? not)))))
  (testing "-s"
    (testing "has errors with invalid method name"
      (let [parsed (parse-options ["-s" "foo" "-f" "deps.edn"])]
        (is (->> parsed :errors empty? not))))
    (testing "has no errors with valid SORT option"
      (let [parsed (parse-options ["-s" "gender" "-f" "deps.edn"])]
        (is (= :gender (-> parsed :options :sort))))))
  (testing "-d"
    (testing "has errors with an unknown or empty DELIM option"
      (let [parsed (parse-options ["-d" "tab" "-f" "deps.edn"])]
        (is (->> parsed :errors :empty? not)))
      (let [parsed (parse-options ["-d" "" "-f" "deps.edn"])]
        (is (->> parsed :errors empty? not))))
    (testing "has no errors with a valid DELIM option"
      (let [parsed (parse-options ["-d" "comma" "-f" "deps.edn"])]
        (is (= :comma (-> parsed :options :delimiter))))))
  (testing "-S has true :silent"
    (let [parsed (parse-options ["-S"])]
      (is (->> parsed :options :silent)))))

(deftest args->initial-state-test
  (testing "-h/--help is :ok?, :help type, and :exit-message"
    (let [state (cli/args->initial-state ["-h"])]
      (is (:ok? state))
      (is (= :help (:exit-type state)))
      (is (->> state :exit-message empty? not))))
  (testing "with errors in opts, not :ok?, :options-error type, :exit-message"
    (let [state (cli/args->initial-state ["-f" "non-existant.txt"])]
      (is (->> state :ok? not))
      (is (= :options-error (:exit-type state)))
      (is (->> state :exit-message empty? not))))
  (testing "with no files specified, not :ok?, :read-without-files type, and :exit-message"
    (let [state (cli/args->initial-state ["read"])]
      (is (->> state :ok? not))
      (is (= :read-without-files (:exit-type state)))
      (is (->> state :exit-message empty? not))))
  (testing "with a valid input, no :exit-message, no :exit-type, has :options"
    (let [state (cli/args->initial-state ["read" "-f" "deps.edn"])]
      (is (->> state :options nil? not))
      (is (nil? (:exit-type state)))
      (is (->> state :exit-message nil?))))
  (testing "has an :action of :read with a 'read' argument"
    (let [state (cli/args->initial-state ["read" "-f" "deps.edn"])]
      (is (= :read (:action state)))))
  (testing "with invalid action, not :ok?, :non-action type, :exit-message mentioning bad action"
    (let [state (cli/args->initial-state ["wrong"])]
      (is (not (:ok? state)))
      (is (= :non-action (:exit-type state)))
      (is (string/includes? (get state :exit-message "no :exit-message") "wrong")))))

(deftest print-people-state-test
  (testing "people data is sent to *out*"
    (let [people-data [["Smith" "John" "Male" "blue" "1/1/2000"]
                       ["Doe" "Jane" "Female" "red" "1/2/2000"]]
          people (map p/person people-data)
          result (with-out-str (cli/print-people-state {:people people}))]
      (is (->> (flatten people-data)
               (map #(string/includes? result %))
               (every? identity)))))
  (testing "error data is sent to *out*"
    (let [errors [{:file "foo.txt" :line 1 :msg "barbaz"}]
          error-data (->> errors first vals (map str))
          result (with-out-str (cli/print-people-state {:errors errors}))]
      (is (->> error-data
               (map #(string/includes? result %))
               (every? identity)))
      (testing "unless :silent"
        (let [silent-result (with-out-str (cli/print-people-state {:errors errors :options {:silent true}}))]
          (println silent-result)
          (is (= '(false false false) (map #(string/includes? silent-result %) error-data))))))))
