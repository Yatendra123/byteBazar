#!/bin/sh
set -e

awslocal sqs create-queue --queue-name order-events || true
awslocal sqs create-queue --queue-name payment-events || true

echo "Queues initialized: order-events, payment-events"
