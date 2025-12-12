flowchart TD

  %% Position control for columns
  S_pad0((( ))):::invisible
  C_pad0((( ))):::invisible
  W_pad0((( ))):::invisible

  %% SERVER - left column
  subgraph SERVER[Server]
    direction TB
    S0((Start))
    S1[Install signal handler]
    S2[Open log file]
    S3[Create server socket]
    S4[Set socket options]
    S5[Bind]
    S6[Listen]
    S7[Accept loop while running]
    S8[Accept client]
    S9[Create worker thread]
    S10[Cleanup and close server socket]
    S11((Stop))

    S0 --> S1 --> S2 --> S3 --> S4 --> S5 --> S6 --> S7
    S7 --> S8 --> S9
    %% S9 --> S7  %% Feedback arrow REMOVED
    S7 -.-> S10 --> S11
  end

  %% CLIENT - center column
  subgraph CLIENT[Client]
    direction TB
    C0((Start))
    C1[Create socket]
    C2[Convert server address]
    C3[Connect]
    C4[Get user input loops]
    C5[Send number one]
    C6[Send number two]
    C7[Send operator]
    C8[Receive response]
    C9[Close socket]
    C10((Stop))

    C0 --> C1 --> C2 --> C3 --> C4 --> C5 --> C6 --> C7 --> C8 --> C9 --> C10
  end

  %% WORKER - right column
  subgraph WORKER[Worker thread per client]
    direction TB
    W0((Thread start))
    W1[Read line one]
    W2[Parse first number]
    W3[Read line two]
    W4[Parse second number]
    W5[Read line three]
    W6[Parse operator]
    W7[Compute result]
    W8[Send result]
    W9[Send error]
    W10[Close client socket]
    W11((Thread end))

    W0 --> W1 --> W2 --> W3 --> W4 --> W5 --> W6 --> W7
    W7 -->|ok| W8 --> W10 --> W11
    W7 -->|error| W9 --> W10 --> W11

    W1 -->|disconnect or error| W10
    W2 -->|invalid| W9
    W4 -->|invalid| W9
    W6 -->|invalid| W9
  end

  %% Arrange columns by invisible links
  S_pad0 ~~~ S0
  C_pad0 ~~~ C0
  W_pad0 ~~~ W0
  S_pad0 ~~~ C_pad0 ~~~ W_pad0

  %% Network lines (explicit, cross column)
  C5 ---|tcp| W1
  C6 ---|tcp| W3
  C7 ---|tcp| W5

  W8 ---|tcp| C8
  W9 ---|tcp| C8

  %% Discontinued (cross) arrow from Create worker thread to Thread start
  S9 --x W0

  %% NEW: Show connect causes server's accept loop to proceed
  C3 -- "TCP SYN" --> S7

  %% Style for invisible anchors
  classDef invisible width:0px,fill:none,stroke:none;