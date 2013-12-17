(ns wikirick2.shell
  (:require [clj-time.core :as clj-time]
            [clj-time.format :as format]
            [clojure.core.match :refer [match]]
            [clojure.java.shell :as shell]
            [clojure.string :as string]
            [slingshot.slingshot :refer :all]
            [wikirick2.types :refer :all]))

(declare parse-co-error
         parse-ci-error
         parse-rcsmerge-err
         rcs-file
         rcs-dir
         parse-date
         date-string
         parse-rlog-output)

(deftype Shell [base-dir])

(defn co-p [shell title rev]
  (let [result (shell/sh "co" (format "-r1.%s" rev) "-p" title :dir (.base-dir shell))]
    (if (= (:exit result) 0)
      (:out result)
      (throw+ (parse-co-error (:err result))))))

(defn write-file [shell title source]
  (spit (format "%s/%s" (.base-dir shell) title) source))

(defn ci [shell title edit-comment]
  (let [result (shell/sh "ci" "-u" title
                         :in edit-comment
                         :dir (.base-dir shell))]
    (if-let [e (parse-ci-error (:err result))]
      (throw+ e))))

(defn rcsmerge [shell title base-revision]
  (let [result (shell/sh "rcsmerge"
                         (format "-r1.%s" base-revision)
                         title
                         :dir (.base-dir shell))]
    (if (not (zero? (:exit result)))
      (throw+ (parse-rcsmerge-err (:err result))))))

(defn ls-rcs-files [shell]
  (let [result (shell/sh "ls" "-t" (rcs-dir shell))
        fnames (string/split-lines (:out result))]
    (for [fname fnames :when (not (empty? fname))]
      (second (re-find #"(.+),v" fname)))))

(defn co-l [shell title]
  (shell/sh "co" "-l" title :dir (.base-dir shell)))

(defn co-u [shell title]
  (shell/sh "co" "-u" "-f" title :dir (.base-dir shell)))

(defn touch-rcs-file [shell title date]
  (let [result (shell/sh "touch" "-d" (date-string date) (rcs-file title) :dir (rcs-dir shell))]
    (if (not (zero? (:exit result)))
      (throw+ {:type :touch-failed :message (:err result)}))))

(defn make-rcs-dir [shell]
  (shell/sh "mkdir" "-p" (rcs-dir shell)))

(defn test-f [shell title]
  (let [result (shell/sh "test" "-f" (rcs-file title) :dir (rcs-dir shell))]
    (= (:exit result) 0)))

(defn rm-rcs-file [shell title]
  (let [result (shell/sh "rm" "-f" (rcs-file title) :dir (rcs-dir shell))]
    (when (not (zero? (:exit result)))
      (throw+ {:type :rm-rcs-file-failed :message (:err result)}))))

(defn rm-co-file [shell title]
  (let [result (shell/sh "rm" "-f" title :dir (.base-dir shell))]
    (when (not (zero? (:exit result)))
      (throw+ {:type :rm-co-file-failed :message (:err result)}))))

(defn rlog [shell title]
  (let [result (shell/sh "rlog" title :dir (.base-dir shell))]
    (if (= (:exit result) 0)
      (parse-rlog-output (:out result))
      (throw+ {:type :rlog-failed}))))

(defn rlog-head [shell title]
  (let [result (shell/sh "rlog" "-h" (rcs-file title) :dir (.base-dir shell))
        parse-head-rev #(Integer/parseInt (second (re-find #"(?m)^head: \d+\.(\d+)" %)))]
    (if (= (:exit result) 0)
      (parse-head-rev (:out result))
      (throw+ {:type :head-revision-failed}))))

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

(defn- parse-ci-error [err]
  (let [message (second (string/split-lines err))]
    (cond (re-find #"\Afile is unchanged;" message) {:type :unchanged-source}
          (or (re-find #"\Anew revision:" message)
              (re-find #"\Ainitial revision:" message)) nil
          :else {:type :unknown-ci-error :message message})))

(defn- parse-rcsmerge-err [err]
  (cond (re-find #"(?m)^rcsmerge: warning: conflicts during merge" err)
        {:type :merge-conflict}
        :else
        {:type :unknown-rcsmerge-error :message err}))

(defn- rcs-file [title]
  (format "%s,v" title))

(defn- rcs-dir [shell]
  (format "%s/RCS" (.base-dir shell)))

(defn- parse-date [date-str]
  (format/parse (format/formatter "yy/MM/dd HH:mm:ss") date-str))

(defn- date-string [date]
  (format/unparse (format/formatter "yyyy-MM-dd HH:mm:ss z") date))

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
