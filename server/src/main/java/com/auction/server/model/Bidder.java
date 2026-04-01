package com.auction.server.model;

import jakarta.persistence.*;

@Entity
@DiscriminatorValue("bidder")
public class Bidder extends User {
}
