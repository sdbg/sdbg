cd bin
rm ../resources/com.github.sdbg.android.forwarder.jar
dx --dex --output=../resources/com.github.sdbg.android.forwarder.jar com/github/sdbg/debug/core/internal/forwarder/*
cd ..
