(ns hxegon.homework.api-test
  (:require
   [clojure.test :refer [deftest testing is]]
   [hxegon.homework.api :as api]
   [muuntaja.core :as m]
   [clojure.pprint :as pp]
   [clojure.string :as string]
   [hxegon.homework.person :as p]))

(deftest routing-sanity-test
  (testing "200 response for"
    (testing "GET /records"
      (let [request {:request-method :get
                     :uri "/records"}
            response (api/app request)]
        (is (= 200 (:status response))))))
  (testing "404 response for GET /non-existent-route"
    (let [request {:request-method :get
                   :uri "/non-existant-route"}
          response (api/app request)]
      (is (= 404 (:status response))))))

(defn post-records
  [body]
  (let [base {:request-method :post
              :uri "/records"
              :headers {"content-type" "application/json"}}
        request (merge base {:body-params body})]
    (api/app request)))

(defn reset-people []
  (reset! api/people []))

(deftest records-test
  (testing "GET /records starts with no people"
    (let [request {:request-method :get
                   :uri "/records"}
          response (api/app request)
          body (m/decode "application/json" (:body response))]
      (is (= [] body))))
  (testing "POST /records returns 400 without required params keys"
    (is (= 400 (:status (post-records {:delimiter "pipe"}))))
    (is (= 400 (:status (post-records {:data "foo"})))))
  (testing "POST /records returns 422 bad param values with invalid delimiter"
    (is (= 422 (:status (post-records {:delimiter "invalid" :data "not evaluated"})))))
  (testing "POST /records with good person data"
    (let [post-response (post-records {:delimiter "pipe"
                                       :data "Smith | John | Male | Blue | 01/01/2001"})
          get-response (api/app {:request-method :get
                                 :uri "/records"})
          records (->> get-response
                       :body
                       (m/decode "application/json"))]
      (testing "is successful"
        (is (= 200 (:status post-response))))
      (testing "get /records is successful"
        (is (= 200 (:status get-response))))
      (testing "is reflected in GET /records"
        (is (= "John" (get (first records) :firstname)))))
    (reset-people))
  (testing "GET /records displays a birthdate as M/D/YYYY"
    (post-records {:delimiter "pipe"
                   :data "Smith | John | Male | Blue | 01/01/2001"})
    (let [resp (api/app {:request-method :get
                         :uri "/records"})
          first-person (first (m/decode-response-body resp))]
      (is (= 200 (:status resp)))
      (is (= "1/1/2001" (:dob first-person)))
    (reset-people))))

(defn sorted-records
  [method]
  (let [request {:request-method :get
                 :uri (str "/records/" method)}]
    (api/app request)))

(def test-people
  (map p/person
       [["Smith" "John" "Male" "Blue" "01/01/2000"]
        ["Doe" "Jane" "Female" "Red" "01/02/2000"]
        ["Zed" "Jake" "Male" "Green" "01/01/1999"]]))

(deftest sorted-records-test
  (run! api/add-person test-people)
  (testing "/records/gender sorts people by gender, female, then male and then by last name"
    (let [resp (sorted-records "gender")]
      (is (= 200 (:status resp)))
      (is (= ["Jane" "John" "Jake"]
             (map :firstname (m/decode-response-body resp))))))
  (testing "/records/name returns records sorted by firstname lastname (ascending)"
    (let [resp (sorted-records "name")]
      (is (= 200 (:status resp)))
      (is (= ["Jake" "Jane" "John"]
             (map :firstname (m/decode-response-body resp))))))
  (testing "/records/birthdate returns records sorted by birthdate (ascending)"
    (let [resp (sorted-records "birthdate")]
      (is (= 200 (:status resp)))
      (is (= ["Jake" "John" "Jane"]
             (map :firstname (m/decode-response-body resp))))))
  (reset-people))
