package com.auction.server.model;

import jakarta.persistence.*;
@DiscriminatorValue("seller")
public class Seller extends User {
}
