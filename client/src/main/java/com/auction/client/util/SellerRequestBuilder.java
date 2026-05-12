package com.auction.client.util;

import com.auction.client.dto.CreateAuctionRequest;
import org.json.JSONObject;

public final class SellerRequestBuilder {

    private SellerRequestBuilder() {
    }

    public static JSONObject buildAuctionBody(CreateAuctionRequest request) {
        JSONObject body = new JSONObject();

        body.put("name", request.productName);
        body.put("type", request.productType);
        body.put("description", request.description);

        if (request.imagePath != null && !request.imagePath.trim().isEmpty()) {
            body.put("imagePath", request.imagePath.trim());
        }

        body.put("startingPrice", request.startingPrice);
        body.put("stepPrice", request.stepPrice);
        body.put("startTime", request.startTime);
        body.put("endTime", request.endTime);
        body.put("sellerId", request.sellerId);

        return body;
    }
}
