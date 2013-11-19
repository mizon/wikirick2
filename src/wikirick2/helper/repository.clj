(ns wikirick2.helper.repository
  (:use slingshot.slingshot
        wikirick2.types)
  (:require [clojure.core.match :refer [match]]
            [clojure.java.shell :as shell]
            [clojure.string :as string]
            [wikirick2.parsers :as parsers]))

(defn- parse-revision [text]
  (let [[_ message] (string/split-lines text)
        [_ rev] (re-find #"^revision \d+\.(\d+)$" message)]
    (Integer. rev)))

(defn- parse-co-error [error-result]
  (match (re-matches #"co: RCS/(.+),v: (.*)" (.trim error-result))
    [_ page-name "No such file or directory"] {:type :page-not-found}
    err {:type :unknown-error :message (str err)}
    :else (assert false "must not happen: parse-co-error")))

(defn select-page- [repo title co-args]
  (let [result (apply shell/sh `("co" ~@co-args :dir ~(.base-dir repo)))]
    (if (= (:exit result) 0)
      (let [rev (parse-revision (:err result))
            page (new-page repo title (:out result))]
        (assoc page :revision rev))
      (throw+ (parse-co-error (:err result))))))

(defmacro with-rw-lock [lock-type & forms]
  `(do
     (.. ~'rw-lock ~lock-type lock)
     (try
       ~@forms
       (finally
         (.. ~'rw-lock ~lock-type unlock)))))

(defn nlinks-per-page-size [page]
  (let [dests (referring-titles page)]
    (Math/round (float (* (/ (count dests) (count (.source page))) 1e6)))))

(defn validate-page-title [title]
  (when (not (parsers/valid-page-name? title))
    (throw+ {:type :invalid-page-title})))
