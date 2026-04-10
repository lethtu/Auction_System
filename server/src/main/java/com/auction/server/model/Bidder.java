package com.auction.server.model;

import jakarta.persistence.*;

@Entity
@DiscriminatorValue("BIDDER")
public class Bidder extends User {
}
