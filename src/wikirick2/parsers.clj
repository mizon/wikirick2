(ns wikirick2.parsers
  (:use [hiccup.core :only [h]]
        zetta.core)
  (:require [clojure.string :as string]
            [zetta.combinators :as c]
            [zetta.parser.seq :as s]))

(declare wiki exec-parser collect-references)

(defn scan-wiki-links [wiki-source]
  (set (map second (re-seq #"\[\[(.+?)\]\]" wiki-source))))

(def ^:dynamic reference-map {})

(defn render-wiki-source [wiki-source]
  (let [[refs lines] (collect-references (string/split-lines wiki-source))]
    (binding [reference-map refs]
      (exec-parser wiki lines))))

;;; Helpers

(defn- exec-parser [parser input]
  {:post [(not= % nil)]}
  (:result (parse-once parser input)))

(defn- unlines [lines]
  (string/join "\n" lines))

(defmacro def- [name value]
  (list `def (with-meta name {:private true}) value))

(def- reference-definition-re #"\s*\[(.*)\]:\s*(.+?)\s*(\".*\")?\s*")

(defn- strip-double-quotes [s]
  (second (re-matches #"\"(.*)\"" s)))

(defn- collect-references [lines]
  (reduce (fn [[map ls] l]
            (let [[_ key href title] (re-matches reference-definition-re l)]
              (if key
                (let [key- (.toLowerCase key)
                      ref (if title
                            {:href (h href) :title (h (strip-double-quotes title))}
                            {:href (h href)})]
                  [(assoc map key- ref) ls])
                [map (conj ls l)])))
          [{} []]
          lines))

;;; Universal combinators

(defn- match-line [regex]
  (fn [input more err-fn ok-fn]
    (let [l (first input)
          matched (and l (re-matches regex l))]
      (if matched
        (ok-fn (rest input) more matched)
        (err-fn input more [] "match-line")))))

(defn- trying [parser]
  (fn [input more err-fn ok-fn]
    (letfn [(err-fn* [_ more* stack msg]
              (err-fn input more* stack msg))]
      (parser input more err-fn* ok-fn))))

(defn- not-followed-by [parser]
  (fn [input more err-fn ok-fn]
    (letfn [(ok-fn* [_ more* result]
              (err-fn input more* [] "not followed by"))

            (err-fn* [_ more* _ _]
              (s/any-token input more* err-fn ok-fn))]
      (parser input more err-fn* ok-fn*))))

;;; Inline elements

(def- escaped
  (trying (>> (s/char \\) s/any-token)))

(defn- surround [start end]
  (trying (do-parser [_ start
                      cs (c/many (<|> escaped (not-followed-by end)))
                      _ end]
            (apply str cs))))

(def- link-body
  (surround (s/char \[) (s/char \])))

(def- inline-link-title
  (c/option nil (do-parser [_ (s/char \")
                            cs (c/many (not-followed-by (s/char \")))
                            _ (s/char \")]
                  (apply str cs))))

(def- inline-link
  (trying (do-parser [body link-body
                      _ (s/char \()
                      url (c/many (not-followed-by (<|> (s/char \)) (s/char \"))))
                      title inline-link-title
                      _ (s/char \))]
            (let [href (h (.trim (apply str url)))
                  attrs (if title
                          {:href href :title (h title)}
                          {:href href})]
              [:a attrs (h body)]))))

(def- reference-link
  (trying (do-parser [body link-body
                      key (surround (s/char \[) (s/char \]))
                      :let [key- (.toLowerCase (if (empty? key)
                                                 body
                                                 key))
                            ref (reference-map key-)]
                      :cond [ref []
                             :else [_ (fail-parser "No reference definition is found")]]]
            [:a ref (h body)])))

(def- strong
  (do-parser [content (<|> (surround (s/string "**") (s/string "**"))
                           (surround (s/string "__") (s/string "__")))]
    [:strong (h content)]))

(def- emphasis
  (do-parser [content (<|> (surround (s/char \*) (s/char \*))
                           (surround (s/char \_) (s/char\_)))]
    [:em (h content)]))

(def- inline-code
  (do-parser [content (surround (s/char\`) (s/char \`))]
    [:code (h content)]))

(def- text
  (do-parser [cs (c/many1 (<|> escaped
                               (not-followed-by (c/choice [inline-link
                                                           reference-link
                                                           strong
                                                           emphasis
                                                           inline-code]))))]
    (h (apply str cs))))

(def- inline-parser
  (c/many (c/choice [text
                     inline-link
                     reference-link
                     strong
                     emphasis
                     inline-code])))

(defn- inline [text]
  (exec-parser inline-parser text))

;;; Block elements

(def- blank-line
  (match-line #"\s*"))

(def- atx-header
  (do-parser [[_ syms content] (match-line #"(#{1,6}) *(.*?) *#*")]
    `[~(keyword (str "h" (count syms))) ~@(inline content)]))

(def- settext-header
  (trying (do-parser [content s/any-token
                      [_ underline] (match-line #"(=+|-+) *")]
            (case (first underline)
              \= `[:h1 ~@(inline content)]
              \- `[:h2 ~@(inline content)]
              (assert false "must not happen")))))

(def- li-indented-cont-line
  (trying (do-parser [es (c/many blank-line)
                      [_ content] (match-line #" {4}(.+)")]
            (if (empty? es)
              content
              ["" content]))))

(defn- li-cont-lines [li-cont-start]
  (let [no-indented (not-followed-by (<|> li-cont-start blank-line))]
    (c/many (<|> li-indented-cont-line no-indented))))

(defn- list-item [li-start li-cont-start]
  (do-parser [[level l] li-start
              ls (li-cont-lines (li-cont-start level))]
    [level (flatten (cons l ls))]))

(defn- list-item-cont [li-cont-start]
  (do-parser [blanks (c/many blank-line)
              [_ l] li-cont-start
              ls (li-cont-lines li-cont-start)]
    (flatten (if (empty? blanks)
               (cons l ls)
               (list* "" l ls)))))

(def- ol-start
  (do-parser [[_ spaces content] (match-line #"( {0,3})\d+\.\s+(.*)")]
    [(count spaces) content]))

(def- ul-start
  (do-parser [[_ spaces content] (match-line #"( {0,3})[\*\+\-]\s+(.*)")]
    [(count spaces) content]))

(def- li-plain-lines
  (do-parser [lines (c/many1 (not-followed-by (<|> ol-start ul-start)))]
    (inline (unlines (map #(.trim %) lines)))))

(declare ordered-list unordered-list)

(def- li-plain
  (delay (c/many (<|> li-plain-lines
                      (<$> list (<|> ordered-list
                                     unordered-list))))))

(defn- list-parser [tag-name li-start li-cont-start]
  (do-parser [[level ls] (list-item li-start li-cont-start)
              lss (c/many (list-item-cont (li-cont-start level)))
              :let [liness (cons ls lss)
                    extractor (if (empty? (filter #(= % "") (flatten liness)))
                                @li-plain
                                (<$> list wiki))]]
    `[~tag-name ~@(for [lines liness]
                    `[:li ~@(apply concat (exec-parser extractor lines))])]))

(def- ordered-list
  (list-parser :ol
               ol-start
               #(match-line (re-pattern (format " {%s}\\d+\\.\\s+(.*)" %)))))

(def- unordered-list
  (list-parser :ul
               ul-start
               #(match-line (re-pattern (format " {%s}[\\*\\+\\-]\\s+(.*)" %)))))

(def- code-line
  (<$> first (match-line #"(\t|    )(.+)")))

(def- block-code
  (do-parser [:let [trim-left #(.replaceAll % "^(\t|    )" "")
                    trim-right #(.replaceAll % "\\s*$" "")]
              l code-line
              ls (c/many (<|> code-line blank-line))
              :let [code-lines (cons l ls)]]
    [:pre [:code (h (trim-right (unlines (map trim-left code-lines))))]]))

(def- bq-marked-line
  (<$> second (match-line #"\s*> ?(.*)")))

(def- bq-no-marked-line
  (not-followed-by blank-line))

(def- bq-fragment
  (do-parser [l bq-marked-line
              ls (c/many (<|> bq-marked-line bq-no-marked-line))
              _ (c/skip-many blank-line)]
    `(~l ~@ls "")))

(def- blockquote
  (do-parser [fragments (c/many1 bq-fragment)]
    `[:blockquote ~@(exec-parser wiki (apply concat fragments))]))

(def- special
  (c/choice [ordered-list
             unordered-list
             block-code
             atx-header
             settext-header
             blockquote
             blank-line]))

(def- paragraph
  (do-parser [ls (c/many1 (not-followed-by special))]
    `[:p ~@(inline (unlines ls))]))

(def- block
  (<|> special paragraph))

(def- wiki
  (do-parser [bs (c/many (*> (c/many blank-line) block))
              _ (c/many blank-line)
              _ s/end-of-input]
    bs))
