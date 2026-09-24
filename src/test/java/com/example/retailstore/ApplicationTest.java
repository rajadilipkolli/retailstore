package com.example.retailstore;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.example.retailstore.security.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

class ApplicationTest {

    @Test
    void productionStartupBootstrapsAdministrator() throws Exception {
        AccountService accountService = mock(AccountService.class);

        new Application().bootstrapProductionAdmin(accountService).run(new DefaultApplicationArguments(new String[0]));

        verify(accountService).bootstrapAdmin();
    }
}
