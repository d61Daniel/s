# PS5 Mod (Fabric 1.21)

## Como compilar o .jar (sem PC)
1. Cria um repositório no GitHub e envia esta pasta toda.
2. Separador **Actions** → workflow **build** → (corre sozinho no push, ou "Run workflow").
3. Quando acabar, abre a execução → **Artifacts** → `ps5mod-jar` → o .jar está lá dentro.
4. Mete o .jar na pasta `mods` junto com o **Fabric API** (versão 0.102.0+1.21).

(No PC: instala Java 21 e Gradle 8.10 e corre `gradle build`; o jar fica em `build/libs/`.)

## Como usar no jogo
- **Craft**
  - Cabo HDMI: ferro, redstone, ferro (numa linha)
  - PS5: 4 quartzo (cantos de cima/baixo e lados), 2 ferro, 1 redstone no centro (QIQ / QRQ / QIQ)
  - TV: ferro ferro ferro / vidro×3 / ferro redstone ferro
- Põe a PS5 e a TV (até 24 blocos de distância).
- Com o cabo: clica na **PS5**, depois na **TV** → ligadas.
- Clica na PS5 (sem cabo) para ligar/desligar.
- Clica na TV: mostra o logo de arranque PS5 e depois o menu com Snake e Pong.
  Se desligares a PS5 e voltares a ligar, arranca de novo.
