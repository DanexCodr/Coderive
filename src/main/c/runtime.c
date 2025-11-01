#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdbool.h>

// Array structure
typedef struct {
    long long size;
    void* data[];
} CoderiveArray;

// Improved print function with better array detection
void runtime_print(long long value) {
    if (value >= -65536 && value <= 65536) { // e.g., -0x10000 to +0x10000
        printf("%lld\n", value);
        fflush(stdout);
        return;
    }

    void* ptr = (void*)value;
    
    // Try array detection first
    CoderiveArray* arr = (CoderiveArray*)ptr;
    
    // Better array detection: check if the pointer might be a valid array
    // by looking at the memory layout more carefully
    if (arr != NULL) {
        // Check if size is reasonable and data pointer looks valid
        if (arr->size >= 0 && arr->size < 1000) {
            printf("[");
            for (long long i = 0; i < arr->size; i++) {
                if (i > 0) printf(", ");
                // Directly print the stored value as integer
                printf("%lld", (long long)(arr->data[i]));
            }
            printf("]\n");
            fflush(stdout);
            return;
        }
    }
    
    // Try as string
    char* str = (char*)ptr;
    if (str != NULL) {
        // Check if first few bytes look like string characters
        int is_string = 1;
        for (int i = 0; i < 32 && str[i] != '\0'; i++) {
            if (str[i] < 32 || str[i] > 126) {
                is_string = 0;
                break;
            }
        }
        
        // --- MODIFIED: Allow printing empty strings ---
        if (is_string) { 
            printf("%s\n", str);
            fflush(stdout);
            return;
        }
    }
    
    // Fallback: print as generic object
    printf("<Object at %p>\n", ptr);
    fflush(stdout);
}

// --- MODIFIED: Made NULL-safe ---
char* string_concat(const char* str1, const char* str2) {
    // --- FIX: Handle NULLs safely ---
    const char* s1 = (str1 == NULL) ? "" : str1;
    const char* s2 = (str2 == NULL) ? "" : str2;
    // --- END FIX ---

    size_t len1 = strlen(s1); // Use s1
    size_t len2 = strlen(s2); // Use s2
    char* result = malloc(len1 + len2 + 1);

    if (result == NULL) {
        perror("malloc failed in string_concat");
        exit(EXIT_FAILURE);
    }

    strcpy(result, s1); // Use s1
    strcat(result, s2); // Use s2
    return result;
}

// --- NEW FUNCTION ---
// Converts a long long integer to a new string
char* runtime_int_to_string(long long value) {
    // 20 digits for 64-bit int + sign + null terminator
    char* str = malloc(22); 
    if (str == NULL) {
        perror("malloc failed in runtime_int_to_string");
        exit(EXIT_FAILURE);
    }
    sprintf(str, "%lld", value);
    return str;
}
// --- END NEW FUNCTION ---


long long runtime_read_input(const char* expected_type) {
    char buffer[256];

    if (fgets(buffer, sizeof(buffer), stdin) == NULL) {
        fprintf(stderr, "Error reading input.\n");
        return 0;
    }

    buffer[strcspn(buffer, "\n")] = 0;

    if (strcmp(expected_type, "string") == 0) {
        char* input_str = malloc(strlen(buffer) + 1);
        if (!input_str) {
            perror("malloc failed in runtime_read_input");
            exit(EXIT_FAILURE);
        }
        strcpy(input_str, buffer);
        return (long long)input_str;
    } else if (strcmp(expected_type, "int") == 0) {
        return strtoll(buffer, NULL, 10);
    } else if (strcmp(expected_type, "float") == 0) {
        float f_val = strtof(buffer, NULL);
        union { float f; long long ll; } u;
        u.f = f_val;
        fprintf(stderr, "Warning: Reading float, returning bit pattern as integer.\n");
        return u.ll;
    } else if (strcmp(expected_type, "bool") == 0) {
        if (strcasecmp(buffer, "true") == 0 || strtol(buffer, NULL, 10) != 0) {
            return 1;
        } else {
            return 0;
        }
    } else {
        fprintf(stderr, "Error: Unknown type requested for input: %s\n", expected_type);
        return 0;
    }
}

CoderiveArray* array_new(long long size) {
    if (size < 0) {
        fprintf(stderr, "Error: array_new called with negative size: %lld\n", size);
        return NULL;
    }
    
    size_t data_size = size * sizeof(void*);
    CoderiveArray* arr = malloc(sizeof(CoderiveArray) + data_size);

    if (arr == NULL) {
        // --- MODIFICATION ---
        // Force flush to stderr to make sure we see this message
        fprintf(stderr, "CRITICAL_ERROR: malloc failed in array_new for size %lld\n", size);
        fflush(stderr); 
        // --- END MODIFICATION ---
        perror("malloc failed in array_new");
        return NULL;
    }

    arr->size = size;
    memset(arr->data, 0, data_size);
    return arr;
}

long long array_load(CoderiveArray* arr, long long index) {
    if (arr == NULL) {
        fprintf(stderr, "Error: array_load called with NULL array pointer.\n");
        return 0;
    }
    if (index < 0 || index >= arr->size) {
        fprintf(stderr, "Error: Array index %lld out of bounds (size %lld).\n", index, arr->size);
        return 0;
    }
    return (long long)(arr->data[index]);
}

void array_store(CoderiveArray* arr, long long index, long long value) {
    if (arr == NULL) {
        fprintf(stderr, "Error: array_store called with NULL array pointer.\n");
        return;
    }
    if (index < 0 || index >= arr->size) {
        fprintf(stderr, "Error: Array index %lld out of bounds for store (size %lld).\n", index, arr->size);
        return;
    }
    arr->data[index] = (void*)value;
}