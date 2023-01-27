# neil-quickadd

## Rationale

`neil-quickadd` gets you started with your new Clojure project faster.
Add your stock dependencies in seconds!

## Usage

`neil-quickadd scan PATH` - Recursively indexes libraries found in toplevel `:deps` from `deps.edn` files a directory.

`neil-quickadd` - Lets you pick a dependency with `fzf`, then adds it with `neil dep add`. Easy!

## FAQ

**Q:** Is neil-quickadd affiliated with Neil?
<br>
**A:** Not really. neil-quickadd uses Neil, but is not endorsed by Neil.

**Q:** Should this be a neil subcommand?
<br>
**A:** Perhaps? Not sure. I say let it marinate for a while on its own first.

## Installing

1. Install Neil: https://github.com/babashka/neil
2. Install fzf: https://github.com/junegunn/fzf
3. Install bbin: https://github.com/babashka/bbin
3. Install neil-quickadd with bbin:

        bbin install io.github.teodorlu/neil-quickadd --latest-sha

## Installing a local development version

Clone the repo,

    git clone https://github.com/teodorlu/neil-quickadd.git
    cd neil-quickadd
        
then install the script with [babashka/bbin][babashka-bbin].

    bbin install .

[babashka-bbin]: https://github.com/babashka/bbin

## Wishlist: Usage from Emacs

I'd like to provide a `neil-quickadd.el` with a `M-x neil-quickadd` function that uses `completing-read` for adding dependencies.
But I haven't done that yet.
Suggestions welcome!
I don't think it should be too much work to wrap `neil-quickadd scan` (for adding) and `neil-quickadd libs` for listing, then selecting from within Emacs.

## Wishlist: Multiple selection

I believe FZF supports selecting multiple options. It would be nice if that just worked! Adding dependencies could be _even faster_.

## Thanks!

Without [@borkdude][borkdude] and [@rads][rads], `neil-quickadd` wouldn't exist. Thank you!

[borkdude]: https://github.com/borkdude/
[rads]: https://github.com/rads/
