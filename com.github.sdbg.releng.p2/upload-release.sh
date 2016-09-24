#!/bin/sh
# upload release repo

# install utils
# https://cloud.google.com/storage/docs/gsutil_install

# Note: allow the public to read the bucket
# gsutil defacl set public-read gs://sdbg


cd com.github.sdbg.releng.p2/target/repository

# upload snapshot
# url locaiton: http://storage.googleapis.com/sdbg/release
gsutil cp -r . gs://sdbg/release