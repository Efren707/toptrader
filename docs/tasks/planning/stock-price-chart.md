# Stock Price Chart (Intraday Line Chart)

> Status: Not yet scoped. High-level backlog entry captured ahead of time — to be broken into GitHub issues, user stories, and acceptance criteria when picked up for implementation.

## Envisioned scope

- A live stock price chart displaying a stock's price movement throughout the trading day.
- A **line chart**: a single price point per interval (closing price or last-traded price), connected by a continuous line — not a candlestick/OHLC chart.
- Goal is a clean, simple view of the day's overall trend, not granular tick-by-tick detail.
- Open questions for scoping time: which interval granularity, live-data source/polling mechanism (ties into the existing Finnhub integration), and which screen(s) it appears on.
