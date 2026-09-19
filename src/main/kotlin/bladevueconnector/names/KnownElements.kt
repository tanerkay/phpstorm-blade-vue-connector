package bladevueconnector.names

/**
 * Element names that are never Vue components: HTML (current and obsolete), SVG, MathML and Vue's
 * built-in components. Compared on [ComponentNames.key], so `<KeepAlive>` and `<keep-alive>` both match.
 */
object KnownElements {
    private const val HTML = """
        a abbr address area article aside audio b base bdi bdo blockquote body br button canvas caption
        cite code col colgroup data datalist dd del details dfn dialog div dl dt em embed fencedframe
        fieldset figcaption figure footer form h1 h2 h3 h4 h5 h6 head header hgroup hr html i iframe img
        input ins kbd label legend li link main map mark menu meta meter nav noscript object ol optgroup
        option output p picture portal pre progress q rp rt ruby s samp script search section select
        selectedcontent slot small source span strong style sub summary sup table tbody td template
        textarea tfoot th thead time title tr track u ul var video wbr
        acronym applet basefont bgsound big blink center dir font frame frameset image isindex keygen
        listing marquee menuitem multicol nextid nobr noembed noframes param plaintext rb rtc spacer
        strike tt xmp
    """

    private const val SVG = """
        svg animate animateMotion animateTransform circle clipPath defs desc discard ellipse feBlend
        feColorMatrix feComponentTransfer feComposite feConvolveMatrix feDiffuseLighting
        feDisplacementMap feDistantLight feDropShadow feFlood feFuncA feFuncB feFuncG feFuncR
        feGaussianBlur feImage feMerge feMergeNode feMorphology feOffset fePointLight
        feSpecularLighting feSpotLight feTile feTurbulence filter foreignObject g hatch hatchpath line
        linearGradient marker mask mesh meshgradient meshpatch meshrow metadata mpath path pattern
        polygon polyline radialGradient rect set solidcolor stop switch symbol text textPath tspan
        unknown use view
        altGlyph altGlyphDef altGlyphItem animateColor color-profile cursor font-face
        font-face-format font-face-name font-face-src font-face-uri glyph glyphRef hkern missing-glyph
        tref vkern
    """

    private const val MATHML = """
        math maction maligngroup malignmark annotation annotation-xml menclose merror mfenced mfrac
        mglyph mi mlabeledtr mlongdiv mmultiscripts mn mo mover mpadded mphantom mprescripts mroot
        mrow ms mscarries mscarry msgroup msline mspace msqrt msrow mstack mstyle msub msubsup msup
        mtable mtd mtext mtr munder munderover none semantics
    """

    private const val VUE_BUILT_INS = """
        component transition transition-group keep-alive teleport suspense router-view router-link
    """

    private val keys: Set<String> = listOf(HTML, SVG, MATHML, VUE_BUILT_INS)
        .flatMap { it.split(Regex("\\s+")) }
        .filter { it.isNotEmpty() }
        .map { ComponentNames.key(it) }
        .toSet()

    fun contains(tagName: String): Boolean = ComponentNames.key(tagName) in keys
}
