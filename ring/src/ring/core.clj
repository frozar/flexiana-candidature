(ns ring.core
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.params :as params :refer [wrap-params]]
            [clojure.core.reducers :as r]))

(def AVAILABLE-LETTER-KEY "available-letters")
(def WORD-KEY "word")

(defn enough-occurencies?
  "Returns true if any key of map m1 is in map m0 and
  if, for a given key, the occurrence of map m1 is inferior or equal
  to occurrence of map m0. Else, returns false.
  Example: (enough-occurencies? {\\r 1 \\e 1} {})      ;; true
           (enough-occurencies? {\\r 1 \\e 1} {\\a 1}) ;; false
           (enough-occurencies? {\\r 1 \\e 1} {\\r 2}) ;; false
           (enough-occurencies? {\\r 1 \\e 1} {\\r 1}) ;; true"
  [m0 m1]
  (loop [item1 (first m1)
         rest-m1 (rest m1)]
    (if (nil? item1)
      true
      (let [[char1 occur1] item1
            occur0 (get m0 char1)]
        (if (or (not occur0)
                (< occur0 occur1))
          false
          (recur (first rest-m1) (rest rest-m1)))))))

(comment
  (let [m0 {\r 1 \e 1}
        m1 {}]
    (enough-occurencies? m0 m1))
  (let [m0 {\r 1, \e 1}
        m1 {\a 1}]
    (enough-occurencies? m0 m1))
  (let [m0 {\r 1, \e 1}
        m1 {\r 2}]
    (enough-occurencies? m0 m1))
  (let [m0 {\r 1, \e 1}
        m1 {\r 1}]
    (enough-occurencies? m0 m1))
  (let [m0 {\r 1, \e 1, \k 1, \q 1, \o 1, \d 1, \l 1, \w 1}
        m1 {\w 1, \o 1, \r 1, \l 1, \d 1}]
    (enough-occurencies? m0 m1)))

(defn quick-frequencies
  "Documentation link:
  https://medium.com/formcept/performance-optimization-in-clojure-using-reducers-and-transducers-a-formcept-exclusive-375955673547"
  [coll]
  (r/fold
   (fn combinef
     ([] {})
     ([x y] (merge-with + x y)))
   (fn reducef
     ([counts x] (merge-with + counts {x 1})))
   coll))

(comment
  (quick-frequencies "rekqodlw")
  (quick-frequencies "world"))

(defn scramble?
  "I don't do a whole lot."
  [available-letter word]
  (let [m0 (quick-frequencies available-letter)
        m1 (quick-frequencies word)]
    (#'enough-occurencies? m0 m1)))

(comment
  (scramble? "rekqodlw" "world")
  (scramble? "cedewaraaossoqqyt" "codewars")
  (scramble? "katas" "steak"))

(defn handler [request]
  (let [error-msg (str "Request parameters (" AVAILABLE-LETTER-KEY " and "
                       WORD-KEY ") required.")
        header {"Content-Type" "text/plain"
                "Access-Control-Allow-Origin" "*"}]
    (if (not= (:request-method request) :post)
      {:status 400
       :headers header
       :body "Only POST requests are allowed."}
      (let [params (:params request)
            available-letter (get params AVAILABLE-LETTER-KEY)
            word (get params WORD-KEY)]
        (if (or (nil? params)
                (nil? available-letter)
                (nil? word))
          {:status 400
           :headers header
           :body error-msg}
          (if (or (nil? (re-matches #"[a-z]*" available-letter))
                  (nil? (re-matches #"[a-z]*" word)))
            {:status 400
             :headers header
             :body "Parameters must contain only lowercase char: a-z"}
            {:status 200
             :headers header
             :body (str (scramble? available-letter word))}))))))

(def app-handler
  (-> #'handler
      (wrap-params {:encoding "UTF-8"})))

(defn -main
  [& args]
  (run-jetty #'app-handler {:port 3000}))

(comment
  (defonce server (run-jetty #'app-handler {:port 3000 :join? false}))
  (.start server)
  (.stop server))
