# Third-party notices

## Google Mobile Ads SDK (AdMob)

- **Artifact:** `com.google.android.gms:play-services-ads` (version 23.6.0).
- **Terms:** the SDK is proprietary Google software governed by the Google Mobile Ads SDK terms and the Google APIs
  Terms of Service. It is not bundled as source and no ad content is shipped with the app.
- **Phase 1:** the app uses the official AdMob **test** app ID and rewarded test unit ID. Production IDs, consent (UMP)
  and the privacy-policy/Data-safety updates are Phase 4 work.

## AndroidSVG

- **Artifact:** `com.caverock:androidsvg-aar` version 1.4.
- **Author:** Paul LeBeau and contributors (<https://github.com/BigBadaboom/androidsvg>).
- **License:** Apache License 2.0 (<https://www.apache.org/licenses/LICENSE-2.0>).
- **Use:** rasterizes the bundled SVG chess-piece sets to bitmaps at runtime; no source is modified.

## Cburnett chess pieces

- **Author:** Colin M.L. Burnett (Wikimedia Commons user [Cburnett](https://commons.wikimedia.org/wiki/User:Cburnett))
- **Files used:** the twelve `wK`, `wQ`, `wR`, `wB`, `wN`, `wP`, `bK`, `bQ`, `bR`, `bB`, `bN`, `bP` chess pieces, bundled
  as `app/src/main/res/drawable/cburnett_*.xml`.
- **Source:** <https://commons.wikimedia.org/wiki/Category:SVG_chess_pieces> (original SVG artwork, created 2006-12-27).
  The files were downloaded from the Lichess repository mirror at
  <https://github.com/lichess-org/lila/tree/master/public/piece/cburnett>.
- **License:** the Commons file pages publish the artwork under **GFDL, BSD and GPL**. This project uses the **BSD**
  option, which the Commons template identifies as the **3-clause BSD license**.
- **Modifications:** the SVG geometry was converted to Android VectorDrawable XML (path data preserved, SVG attributes
  mapped to Android equivalents). No artistic changes were made. Other project files may reuse, modify or redistribute
  the artwork under the same license.

### BSD 3-Clause License

Copyright (c) 2006, Colin M.L. Burnett
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its
   contributors may be used to endorse or promote products derived from
   this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

## Chessnut chess pieces

- **Author:** Alexis Luengas (<https://github.com/LexLuengas>).
- **Files used:** the twelve `wK`, `wQ`, `wR`, `wB`, `wN`, `wP`, `bK`, `bQ`, `bR`, `bB`, `bN`, `bP` SVGs, bundled as
  `app/src/main/assets/pieces/chessnut/`.
- **Source:** <https://github.com/lichess-org/lila/tree/master/public/piece/chessnut>; upstream
  <https://github.com/LexLuengas/chessnut-pieces>.
- **License:** Apache License 2.0 (<https://www.apache.org/licenses/LICENSE-2.0>). The upstream
  `LICENSE.txt` and the Lichess copying table both list Apache-2.0.
- **Modifications:** none beyond downloading the SVG files.

## Fantasy, Celtic and Spatial chess pieces

- **Author:** Maurizio Monge (<https://github.com/maurimo/chess-art>).
- **Files used:** the twelve `wK`, `wQ`, `wR`, `wB`, `wN`, `wP`, `bK`, `bQ`, `bR`, `bB`, `bN`, `bP` SVGs in each of
  `app/src/main/assets/pieces/fantasy/`, `app/src/main/assets/pieces/celtic/` and
  `app/src/main/assets/pieces/spatial/`.
- **Source:** <https://github.com/lichess-org/lila/tree/master/public/piece/{fantasy,celtic,spatial}>; upstream
  <https://github.com/maurimo/chess-art>.
- **License:** MIT (upstream `LICENSE` and the Lichess copying table).
- **Modifications:** the Spatial files had the root `width`/`height`/`style` attributes removed and their inherited
  gradient stops copied into each gradient so AndroidSVG 1.4 can rasterize them; no artistic changes were made.

### MIT License

Copyright (c) Maurizio Monge

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

