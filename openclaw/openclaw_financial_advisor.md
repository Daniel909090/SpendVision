# OpenClaw Financial Advisor Workflow

## Purpose

This workflow uses OpenClaw as an AI financial analysis agent for SpendVision.

SpendVision exports structured receipt data as JSON. OpenClaw reads this file and analyses user spending behaviour to generate a financial summary, category breakdown, risk areas, and practical recommendations.

## Input

The workflow expects a JSON file exported from the Android app:

```text
spendvision_openclaw_export.json