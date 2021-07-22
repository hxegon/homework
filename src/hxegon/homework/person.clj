(ns hxegon.homework.person
  (:require
    [clojure.string :as string]
    [hxegon.homework.internal :refer [key-of-m? safe-parse]]))

(defrecord Person [lastname firstname gender fav-color dob])

(def dob-format (java.text.SimpleDateFormat. "M/d/yyyy"))

(defn parse-dob
  "Parse a MM/dd/yyyy formatted date string"
  [dob-string]
  (safe-parse dob-format dob-string))

(defn person
  "Constructor for Person with validation. Returns either Person or map with :errors"
  [args]
  (if (not= 5 (count args))
    {:error-msg (str "Expected 5 values but got " (count args) " from " (string/join ", " args))}
    (let [[lastname firstname gender fav-color dob-string] args
          dob (parse-dob dob-string)]
      (if (:error-msg dob)
        dob
        (->Person lastname firstname gender fav-color (:token dob))))))

(def delimiters
  {:pipe #" \| "
   :comma #", "
   :space #" "})

(defn valid-delimiter? [d]
  (key-of-m? delimiters d))

(defn line->person
  "parse a line into a person. returns either a person or {:error-msg string}"
  [delim line]
  (if (valid-delimiter? delim)
    (person (string/split line (delim delimiters)))
    {:error-msg (str "Invalid delimiter value: " delim)}))

(def people-sorters
  "map of fns that sort collections of people"
  {:lastname
   (fn lastname-sorter [people]
     (reverse (sort-by :lastname people)))
   :birthdate
   (fn birthdate-sorter [people]
     (sort-by :dob people))
   :gender 
   (fn gender-sorter [people]
     (->> people
          (sort-by :lastname)
          (sort-by (comp string/lower-case :gender))))
   :name
   (fn name-sorter [people]
     (sort-by #(->> %
                    ((juxt :firstname :lastname))
                    (string/join " ")
                    string/lower-case)
              people))})

(defn valid-sorter? [s]
  (key-of-m? people-sorters s))

(defn render-dob
  "Renders a date according to dob-format"
  [date]
  (.format dob-format date))
