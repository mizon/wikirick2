(ns wikirick2.repository.helper
  (:use wikirick2.types)
  (:require [clojure.java.shell :as shell]
            [clojure.string :as string]))

(defn- parse-revision [text]
  (let [[_ message] (string/split-lines text)
        [_ rev] (re-find #"^revision \d+\.(\d+)$" message)]
    (Integer. rev)))

(defn select-page- [repo title co-args]
  (let [result (apply shell/sh `("co" ~@co-args :dir ~(.base-dir repo)))]
    (if (= (:exit result) 0)
      (let [rev (parse-revision (:err result))
            page (make-page title (:out result))]
        (assoc page :revision rev))
      (throw (RuntimeException. (:err result))))))

(defmacro with-rw-lock [lock-type & forms]
  `(do
     (.. ~'rw-lock ~lock-type lock)
     (try
       ~@forms
       (finally
         (.. ~'rw-lock ~lock-type unlock)))))
