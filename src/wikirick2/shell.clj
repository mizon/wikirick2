(ns wikirick2.shell
  (:require [clj-time.core :as clj-time]
            [clj-time.format :as format]
            [clojure.core.match :refer [match]]
            [clojure.java.shell :as shell]
            [clojure.string :as string]
            [slingshot.slingshot :refer :all]
            [wikirick2.types :refer :all]))

(declare parse-co-error
         rcs-file
         rcs-dir
         parse-date
         parse-rlog-output)

(deftype Shell [base-dir])

(defn co-p [shell title rev]
  (let [result (shell/sh "co" (format "-r1.%s" rev) "-p" title :dir (.base-dir shell))]
    (if (= (:exit result) 0)
      (:out result)
      (throw+ (parse-co-error (:err result))))))

(defn ci [shell title source edit-comment]
  (spit (format "%s/%s" (.base-dir shell) title) source)
  (let [result (shell/sh "ci" "-u" title
                         :in edit-comment
                         :dir (.base-dir shell))]
    (when (not= (:exit result) 0)
      (throw+ {:type :ci-failed}))))

(defn head-revision [shell title]
  (let [result (shell/sh "head" (rcs-file title) :dir (rcs-dir shell))
        parse-revision #(Integer/parseInt (second (re-find #"head\s+\d+\.(\d+);" %)))]
    (if (= (:exit result) 0)
      (parse-revision (first (string/split-lines (:out result))))
      (throw+ {:type :head-revision-failed}))))

(defn ls-rcs-files [shell]
  (let [result (shell/sh "ls" "-t" (rcs-dir shell))
        fnames (string/split-lines (:out result))]
    (for [fname fnames :when (not (empty? fname))]
      (second (re-find #"(.+),v" fname)))))

(defn co-l [shell title]
  (shell/sh "co" "-l" title :dir (.base-dir shell)))

(defn make-rcs-dir [shell]
  (shell/sh "mkdir" "-p" (rcs-dir shell)))

(defn test-f [shell title]
  (let [result (shell/sh "test" "-f" (rcs-file title) :dir (rcs-dir shell))]
    (= (:exit result) 0)))

(defn rlog [shell title]
  (let [result (shell/sh "rlog" title :dir (.base-dir shell))]
    (if (= (:exit result) 0)
      (parse-rlog-output (:out result))
      (throw+ {:type :rlog-failed}))))

(defn rlog-date [shell title rev]
  (let [rev-opt (format "-r1.%s" rev)
        result (shell/sh "rlog" rev-opt title :dir (.base-dir shell))]
    (if (= (:exit result) 0)
      (let [[_ date-str] (re-find #"(?m)^date: (.+?);" (:out result))]
        (parse-date date-str))
      (throw+ {:type :rlog-date-failed}))))

(defn grep-iF [shell word]
  (let [command (format "grep -iF --exclude-dir RCS '%s' *" (.replace word "'" "'\\''"))
        result (shell/sh "sh" "-c" command :dir (.base-dir shell))
        merge-line (fn [m l]
                     (match (re-matches #"(.+?)\:(.+)" l)
                       [_ name content] (merge {name content} m)
                       :else m))]
    (if (empty? (:err result))
      (set (reduce merge-line {} (string/split-lines (:out result))))
      (throw+ {:type :grep-iF-failed :message (:err result)}))))

(defn rcsdiff [shell title from-rev to-rev]
  (let [result (shell/sh "rcsdiff"
                         "-u"
                         (format "-r1.%s" from-rev)
                         (format "-r1.%s" to-rev)
                         title
                         :dir (.base-dir shell))]
    (if (= (:exit result) 1)
      (re-find #"(?ms)^@@.*\Z" (:out result))
      (throw+ {:type :rcsdiff-failed :message (:err result)}))))

(defn- parse-co-error [error-result]
  (match (re-matches #"co: RCS/(.+),v: (.*)" (.trim error-result))
    [_ page-name "No such file or directory"] {:type :page-not-found}
    err {:type :unknown-error :message (str err)}
    :else (assert false "must not happen: parse-co-error")))

(defn- rcs-file [title]
  (format "%s,v" title))

(defn- rcs-dir [shell]
  (format "%s/RCS" (.base-dir shell)))

(defn- parse-date [date-str]
  (format/parse (format/formatter "yy/MM/dd HH:mm:ss") date-str))

(defn- parse-rlog-output [output]
  (map (fn [[rev props]]
         (let [[_ date] (re-matches #"date: (.+?);.*" props)
               [lines added deleted] (re-matches #".*?lines: \+(\d+) -(\d+)" props)
               [_ rev] (re-matches #"revision 1.(\d+)" rev)]
           (let [m {:date (parse-date date)
                    :revision (Integer/parseInt rev)}]
             (if lines
               (assoc m :lines {:added (Integer/parseInt added)
                                :deleted (Integer/parseInt deleted)})
               m))))
       (partition 2
                  (filter identity
                          (map #(first (re-matches #"(revision 1.\d+|date: .+)" %))
                               (string/split-lines output))))))
