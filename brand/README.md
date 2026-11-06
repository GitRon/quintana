<p align="center"><img src="quintana-readme-banner.png" alt="Quintana" width="720"></p>

# Quintana brand

A compact heraldic identity: a stopwatch face whose hand is the pivoting arm of the quintain,
the dummy knights trained on, with a shield as its counterweight. Whoever hit the quintain had to
move on before its arm swung back, so the arm doubles as a clock hand.

## Palette

| Token | Hex | Use |
| --- | --- | --- |
| Charcoal | `#1B1B1B` | Ground of every screen and of the icon |
| Antique gold | `#D4AF37` | Case and crown of the mark, the end of a round, accents, save |
| Ivory | `#F5EAD7` | Text, ticks, arm and shield |
| Deep red | `#B22222` | Only the hub of the mark |

The timer phases keep their own, brighter tinctures so iron-colored text on the big buttons stays
readable: workout `#D9442E`, rest `#4C8FD6`, end of a round in antique gold.

## Typography

**Cinzel** for the wordmark and screen titles, **Barlow Condensed** for numbers and labels,
**Barlow** for the rest of the app (all SIL Open Font License). The SVG lockups reference Cinzel
without embedding it; the PNGs here are rendered with it.

## Files

| Asset | File | Use |
| --- | --- | --- |
| Mark | `logo/quintana-mark.svg` | Master of the stopwatch alone |
| Logo, dark / light ground | `logo/quintana-logo-dark.svg`, `-light.svg` (+ PNG) | Website, documents |
| App icon | `icon/quintana-app-icon.svg` (rounded), `-square.svg` | Masters; the launcher uses the adaptive icon in `app/src/main/res` |
| Store icon | `icon/quintana-store-icon-512.png` | Play Store listing (512 × 512, Google applies the mask) |
| Splash | `splash/quintana-splash.svg` (+ PNG) | Store graphics; Android 12+ shows the adaptive icon on charcoal instead |
| README banner | `quintana-readme-banner.svg` (+ PNG) | Top of the README, social preview |

The app draws the mark as vector drawables: `logo.xml` in full color, the launcher foreground
scaled into the adaptive icon's safe zone, and a simplified one-color version without ticks and
with a solid shield for the themed icon and the status bar, where fine lines would blur.

Based on a brand kit drafted with ChatGPT. In the lockups the shield is filled with the ground
color, so the arm does not show through it, and the banner's wordmark is set smaller so Cinzel
fits.
