// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// Things that should be "auto-generated" are between AUTO START and
// AUTO END (or just AUTO if it's a single line).


class Mint {
  Mint() : registry_ = new Map<SendPort, Purse>() {
    // AUTO START
    ReceivePort mintPort = new ReceivePort();
    port = mintPort.toSendPort();
    serveMint(mintPort);
    // AUTO END
  }

  // AUTO START
  void serveMint(ReceivePort port) {
    port.receive((var message, SendPort replyTo) {
      int balance = message;
      Purse purse = createPurse(balance);
      replyTo.send([ purse.port ], null);
    });
  }
  // AUTO END

  Purse createPurse(int balance) {
    Purse purse = new Purse(this, balance);
    registry_[purse.port] = purse;
    return purse;
  }

  Purse lookupPurse(SendPort port) {
    return registry_[port];
  }

  Map<SendPort, Purse> registry_;
  // AUTO
  SendPort port;
}


// AUTO START
class MintWrapper {
  MintWrapper(SendPort this.mint_) {}

  void createPurse(int balance, handlePurse(PurseWrapper purse)) {
    mint_.call(balance).receive((var message, SendPort replyTo) {
      SendPort purse = message[0];
      handlePurse(new PurseWrapper(purse));
    });
  }

  SendPort mint_;
}
// AUTO END


/*
One way this could look without the autogenerated code:

class Mint {
  Mint() : registry_ = new Map<SendPort, Purse>() {
  }

  wrap Purse createPurse(int balance) {
    Purse purse = new Purse(this, balance);
    registry_[purse.port] = purse;
    return purse;
  }

  Purse lookupPurse(SendPort port) {
    return registry_[port];
  }

  Map<SendPort, Purse> registry_;
}

The other end of the port would use Wrapper<Mint> as the wrapper, or
Future<Mint> as a future for the wrapper.
*/


class Purse {
  Purse(Mint this.mint, int this.balance) {
    // AUTO START
    ReceivePort recipient = new ReceivePort();
    port = recipient.toSendPort();
    servePurse(recipient);
    // AUTO END
  }

  // AUTO START
  void servePurse(ReceivePort recipient) {
    recipient.receive((var message, SendPort replyTo) {
      String command = message[0];
      if (command == "balance") {
        replyTo.send(queryBalance(), null);
      } else if (command == "deposit") {
        Purse source = mint.lookupPurse(message[2]);
        deposit(message[1], source);
      } else if (command == "sprout") {
        Purse result = sproutPurse();
        replyTo.send([ result.port ], null);
      } else {
        // TODO: Send an exception back.
        replyTo.send("Exception: Command not understood", null);
      }
    });
  }
  // AUTO END

  int queryBalance() { return balance; }

  Purse sproutPurse() { return mint.createPurse(0); }

  void deposit(int amount, Purse source) {
    // TODO: Throw an exception if the source purse doesn't hold
    // enough dough.
    balance += amount;
    source.balance -= amount;
  }

  Mint mint;
  int balance;
  // AUTO
  SendPort port;
}


// AUTO START
class PurseWrapper {
  PurseWrapper(SendPort this.purse_) {}

  void queryBalance(handleBalance(int balance)) {
    purse_.call([ "balance" ]).receive((var message, SendPort replyTo) {
      int balance = message;
      handleBalance(balance);
    });
  }

  void sproutPurse(handleSprouted(PurseWrapper sprouted)) {
    purse_.call([ "sprout" ]).receive((var message, SendPort replyTo) {
      SendPort sprouted = message[0];
      handleSprouted(new PurseWrapper(sprouted));
    });
  }

  void deposit(PurseWrapper source, int amount) {
    purse_.send([ "deposit", amount, source.purse_ ], null);
  }


  SendPort purse_;
}
// AUTO END


// AUTO STATUS UNCLEAR!

class MintMakerWrapperIsolate extends Isolate {
  MintMakerWrapperIsolate() : super() { }
  void main() {
    this.port.receive((var message, SendPort replyTo) {
      Mint mint = new Mint();
      replyTo.send([ mint.port ], null);
    });
  }
}

class MintMakerWrapper {
  MintMakerWrapper() {
    port_ = new MintMakerWrapperIsolate().spawn();
  }

  void makeMint(handleMint(MintWrapper mint)) {
    port_.then((SendPort port) {
      port.call(null).receive((var message, SendPort replyTo) {
        SendPort mint = message[0];
        handleMint(new MintWrapper(mint));
      });
    });
  }

  Promise<SendPort> port_;
}


class MintMakerTest {
  static void testMain() {
    MintMakerWrapper mintMaker = new MintMakerWrapper();
    mintMaker.makeMint((MintWrapper mint) {
      mint.createPurse(100, (PurseWrapper purse) {
        purse.queryBalance((int balance) {
          Expect.equals(100, balance);
        });
        purse.sproutPurse((PurseWrapper sprouted) {
          sprouted.queryBalance((int balance) {
            Expect.equals(0, balance);
          });
          sprouted.deposit(purse, 5);
          sprouted.queryBalance((int balance) {
            Expect.equals(0 + 5, balance);
          });
          purse.queryBalance((int balance) {
            Expect.equals(100 - 5, balance);
          });
          sprouted.deposit(purse, 42);
          sprouted.queryBalance((int balance) {
            Expect.equals(0 + 5 + 42, balance);
          });
          purse.queryBalance((int balance) {
            Expect.equals(100 - 5 - 42, balance);
          });
        });
      });
    });
  }

  /* This is an attempt to show how the above code could look like if we had
   * better language support for asynchronous messages (deferred/asynccall).
   * The static helper methods like createPurse and queryBalance would also
   * have to be marked async.

  void run(port) {
    MintMakerWrapper mintMaker = spawnMintMaker();
    deferred {
      MintWrapper mint = asynccall mintMaker.createMint();
      PurseWrapper purse = asynccall mint.createPurse(100);
      Expect.equals(100, asynccall purse.queryBalance());

      PurseWrapper sprouted = asynccall purse.sproutPurse();
      Expect.equals(0, asynccall sprouted.queryBalance());

      asynccall sprouted.deposit(purse, 5);
      Expect.equals(0 + 5, asynccall sprouted.queryBalance());
      Expect.equals(100 - 5, asynccall purse.queryBalance());

      asynccall sprouted.deposit(purse, 42);
      Expect.equals(0 + 5 + 42, asynccall sprouted.queryBalance());
      Expect.equals(100 - 5 - 42, asynccall purse.queryBalance());
    }
  }
  */

  /* And a version using futures and wrappers.

  void run(port) {
    Wrapper<MintMaker> mintMaker = spawnMintMaker();
    Future<Mint> mint = mintMaker...createMint();
    Future<Purse> purse = mint...createPurse(100);
    Expect.equals(100, purse.queryBalance());

    Future<Purse> sprouted = purse...sproutPurse();
    Expect.equals(0, sprouted.queryBalance());

    sprouted...deposit(purse, 5);
    Expect.equals(0 + 5, sprouted.queryBalance());
    Expect.equals(100 - 5, purse.queryBalance());

    sprouted...deposit(purse, 42);
    Expect.equals(0 + 5 + 42, sprouted.queryBalance());
    Expect.equals(100 - 5 - 42, purse.queryBalance());
  }
  */

}

main() {
  MintMakerTest.testMain();
}
