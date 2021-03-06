(ns hxegon.homework.internal)

(defn key-of-m?
  "Check if v is a key of m. Returns k if true or nil if false."
  [m k]
  ((into #{} (keys m)) k))

(defn safe-parse
  "Wrapper for .parse that returns :token or :error-msg (instead of throwing ParseException)"
  [fmt string]
  (try
    {:token (.parse fmt string)}
    (catch java.text.ParseException e
      {:error-msg (ex-message e)})))
