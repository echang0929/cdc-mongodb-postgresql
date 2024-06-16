package com.tutorial.cdc.product.service;

import com.tutorial.cdc.product.entity.Product;
import com.tutorial.cdc.product.repository.ProductRepository;
import io.debezium.data.Envelope.Operation;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public void handleEvent(Operation operation, String documentId, String collection, Product product) {

        // Check if the operation is either CREATE or READ
        if (operation == Operation.CREATE || operation == Operation.READ) {
            // Set the MongoDB document ID to the product
            product.setMongoId(documentId);
            product.setSourceCollection(collection);
            // Save the updated product information to the database
            productRepository.save(product);

            // If the operation is UPDATE
        } else if (operation == Operation.UPDATE) {
            var productToUpdate = productRepository.findByMongoId(documentId);
            product.setId(productToUpdate.getId());
            product.setMongoId(documentId);
            product.setSourceCollection(collection);
            productRepository.save(product);
        }
        // If the operation is DELETE
        else if (operation == Operation.DELETE) {
            // Remove the product from the database using the MongoDB document ID
            productRepository.removeProductByMongoId(documentId);
        }
    }
}