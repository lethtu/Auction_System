package com.auction.server.model;

import jakarta.persistence.*;

@Entity
@DiscriminatorValue("bidder")
public class bidder extends user{
}
