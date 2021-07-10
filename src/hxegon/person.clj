(ns hxegon.person)

(defrecord Person [lastname firstname gender fav-color dob])

(defn ^:private safe-parse
  "Wrapper for .parse that returns :token or :error-msg instead of throwing ParseException"
  [fmt string]
  (try
    {:token (.parse fmt string)}
    (catch java.text.ParseException e
      {:error-msg (ex-message e)})))

(defn person
  "Constructor for Person with validation. Returns either Person or map with :errors"
  [args]
  (if (not= 5 (count args))
    {:errors [(str "Expected 5 arguments but got " (count args))]}
    (let [[lastname firstname gender fav-color dob-string] args
          date-format (java.text.SimpleDateFormat. "MM/dd/yyyy")
          dob (safe-parse date-format dob-string)]
      (if (:error-msg dob)
        {:errors [(:error-msg dob)]}
        (->Person lastname firstname gender fav-color (:token dob))))))
