Wikirick2
=========
A [RCS][]-based wiki engine for personal sites. This software is in development and beta quality yet.

Features
--------
* Implemented in [Clojure][]
* RCS-based
* Keeping revision history and able to see them
* Adopting the Markdown syntax
* Linking automatically between any related pages

Requirements
------------
* [Clojure][] 1.5+
* [Leiningen][] 2.x
* [GNU RCS][RCS] 5.0+
* [SQLite][] 3.5+
* Linux (I will support other platforms in the future)

Installation
------------
Before installation, you must get Leiningen which is a package manager for Clojure programming language.

After installed Leiningen, clone this repository and type the next command in the direcotry please:

    $ lein ring server

Wikirick2 will start serving, and you can see the site on `http://localhost:3000/` .

License
-------
Wikirick2 is distributed under the [BSD 3-Clause License][BSD3].

[RCS]: http://www.gnu.org/software/rcs/
[Clojure]: http://clojure.org/
[Leiningen]: http://leiningen.org/
[SQLite]: http://www.sqlite.org/
[BSD3]: http://opensource.org/licenses/BSD-3-Clause
