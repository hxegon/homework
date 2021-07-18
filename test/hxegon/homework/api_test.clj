(ns hxegon.homework.api-test
  (:require
    [clojure.test :refer [deftest testing is]]
    [hxegon.homework.api :as api]
    [muuntaja.core :as m]
    [clojure.pprint :as pp]))

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
        (is (= "John" (get (first records) :firstname)))))))
