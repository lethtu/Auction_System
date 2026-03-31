package com.auction.server.model;

import jakarta.persistence.*;
@DiscriminatorValue("seller")
public class seller extends user{
}
