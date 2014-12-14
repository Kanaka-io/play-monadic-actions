/*!
 Deck JS - deck.fit
 Copyright (c) 2012 Rémi Emonet
 Licensed under the MIT license.
 https://github.com/imakewebthings/deck.js/blob/master/MIT-license.txt
 */

/*
 This extension provides a way of scaling the slides to fit the slide container.
 A "design size" is used to do global scaling of all slides in the same way.
 The default design size is 800x600.
 */
(function($, deck, window, undefined) {
    var $d = $(document),
        $w = $(window),
        timer, // Timeout id for debouncing
        rootSlides,

    /*
     Internal function to do all the dirty work of scaling the slides.
     */
        scaleDeck = function() {
            var opts = $[deck]('getOptions');
            var addMarginX = opts.fitMarginX * 2;
            var addMarginY = opts.fitMarginY * 2;
            var fitMode = opts.fitMode;
            var sdw = opts.designWidth;
            var sdh = opts.designHeight;
            var $container = $[deck]('getContainer');
            var scaleX = $container.hasClass(opts.classes.globalscale) ? $container.innerWidth() / (sdw+addMarginX) : 1;
            var scaleY = $container.hasClass(opts.classes.globalscale) ? $container.innerHeight() / (sdh+addMarginY) : 1;
            //var truescaleX = $container.hasClass(opts.classes.globalscale) ? $container.innerWidth() / (sdw) : 1;
            //var truescaleY = $container.hasClass(opts.classes.globalscale) ? $container.innerHeight() / (sdh) : 1;
            var scale = scaleX < scaleY ? scaleX : scaleY;
            var rootSlides = [];
            var slideTest = $.map([opts.classes.before, opts.classes.previous, opts.classes.current, opts.classes.next, opts.classes.after],
                function(el, i) {return '.' + el;}).join(', ');

            $.each($[deck]('getSlides'), function(i, $el) {
                if (!$el.parentsUntil(opts.selectors.container).length) {
                    rootSlides.push($el);
                }
            });
            $.each(rootSlides, function(i, $slide) {
                $slide.css('width', sdw);
                $slide.css('height', sdh);
                $.each('Webkit Moz O ms Khtml'.split(' '), function(i, prefix) {
                    if (scale == 1) {
                        $slide.css(prefix + 'Transform', '');
                    } else {
                        if (fitMode == "left top" || fitMode == "top left") {
                            // ok align left/top (ok with the percents)
                            $slide.css(prefix + 'Transform', 'translate(-50%,-50%) scale(' + scale + ' , ' + scale + ') translate(50%,50%) translate('+(addMarginX/2)+'px,'+(addMarginY/2)+'px)');
                        } else if (fitMode == "center middle") {
                            // ok align center/middle
                            $slide.css(prefix + 'Transform', 'translate(-50%,-50%) scale(' + scale + ' , ' + scale + ') translate(50%, 50%) translate('+($container.innerWidth()/2/scale - sdw/2)+'px,'+($container.innerHeight()/2/scale - sdh/2)+'px)');
                        } else if (fitMode == "right bottom" || fitMode == "bottom right") {
                            // ok align right/bottom
                            $slide.css(prefix + 'Transform', 'translate(-50%,-50%) scale(' + scale + ' , ' + scale + ') translate(50%, 50%) translate('+($container.innerWidth()/scale - sdw - addMarginX/2)+'px,'+($container.innerHeight()/scale - sdh - addMarginY/2)+'px)');
                        } else if (fitMode == "stretched") {
                            // ok stretched (with respect of the margin, i.e., it is center/middle)
                            $slide.css(prefix + 'Transform', 'scale(' + scaleX + ' , ' + scaleY + ') translate('+(($container.innerWidth()-sdw)/2/scaleX)+'px,'+(($container.innerHeight()-sdh)/2/scaleY)+'px)');
                        }
                    }
                });
            });

        }

    /*
     Extends defaults/options.

     options.designWidth
     Defaults to 800. You may instead specify a width as a number
     of px and all slides will be scaled in the same way, considering their
     size is the provided one.

     options.designHeight
     Defaults to 600. You may instead specify a height as a number
     of px and all slides will be scaled in the same way, considering their
     size is the provided one.

     options.fitMode
     How to adapt the slide to the container.
     Only the following combinations are available for now:
     "center middle", "top left", "bottom right", "stretched"

     options.fitMarginX
     options.fitMarginY
     Defaults to 5. Adds some margin in design space units.
     E.g., if the designe width is 800 and the margin is 5, the slide will be
     810 pixel wide before rescaling.

     options.scaleDebounce
     Scaling on the browser resize event is debounced. This number is the
     threshold in milliseconds. You can learn more about debouncing here:
     http://unscriptable.com/index.php/2009/03/20/debouncing-javascript-methods/

     */
    $.extend(true, $[deck].defaults, {
        classes: {
            globalscale: 'deck-globalscale'
        },
        keys: {
            scale: 83 // s
        },

        designWidth: 1360,
        designHeight: 768,
        fitMode: "stretch",
        fitMarginX: 0,
        fitMarginY: 0,
        scaleDebounce: 200
    });

    /*
     jQuery.deck('disableScale')

     Disables scaling and removes the scale class from the deck container.
     */
    $[deck]('extend', 'disableScale', function() {
        $[deck]('getContainer').removeClass($[deck]('getOptions').classes.globalscale);
        scaleDeck();
    });

    /*
     jQuery.deck('enableScale')

     Enables scaling and adds the scale class to the deck container.
     */
    $[deck]('extend', 'enableScale', function() {
        $[deck]('getContainer').addClass($[deck]('getOptions').classes.globalscale);
        scaleDeck();
    });

    /*
     jQuery.deck('toggleScale')

     Toggles between enabling and disabling scaling.
     */
    $[deck]('extend', 'toggleScale', function() {
        var $c = $[deck]('getContainer');
        $[deck]($c.hasClass($[deck]('getOptions').classes.globalscale) ?
            'disableScale' : 'enableScale');
    });

    $d.bind('deck.init', function() {
        var opts = $[deck]('getOptions');

        // Debounce the resize scaling
        $w.unbind('resize.deckscale').bind('resize.deckscale', function() {
            window.clearTimeout(timer);
            timer = window.setTimeout(scaleDeck, opts.scaleDebounce);
        })
            // Scale once on load, in case images or something change layout
            .unbind('load.deckscale').bind('load.deckscale', scaleDeck);

        // Bind key events
        $d.unbind('keydown.deckscale').bind('keydown.deckscale', function(e) {
            if (e.which === opts.keys.scale || $.inArray(e.which, opts.keys.scale) > -1) {
                $[deck]('toggleScale');
                e.preventDefault();
            }
        });

        // Enable scale on init
        $[deck]('enableScale');
    });
})(jQuery, 'deck', this);

