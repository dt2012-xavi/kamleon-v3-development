package com.dynatech2012.kamleonuserapp.views

import androidx.compose.foundation.background
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.dynatech2012.kamleonuserapp.R

@Composable
fun AuthScreenLayout(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.white))
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = dimensionResource(R.dimen.auth_horizontal_padding)),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}

@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    testTag: String,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    modifier: Modifier = Modifier,
    password: Boolean = false,
    onImeAction: (() -> Unit)? = null
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth().testTag(testTag),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge,
        shape = CircleShape,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onImeAction?.invoke() }),
        visualTransformation = if (password && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = if (password) {
            {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        painter = painterResource(if (passwordVisible) R.drawable.ic_eye_open else R.drawable.ic_eye_crossed),
                        contentDescription = stringResource(if (passwordVisible) R.string.auth_hide_password else R.string.auth_show_password),
                        tint = colorResource(R.color.kamleon_secondary_grey_60)
                    )
                }
            }
        } else null,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colorResource(R.color.kamleon_blue_dark),
            unfocusedBorderColor = colorResource(R.color.kamleon_secondary_grey_40),
            focusedLabelColor = colorResource(R.color.kamleon_blue_dark),
            unfocusedLabelColor = colorResource(R.color.kamleon_secondary_grey_60),
            cursorColor = colorResource(R.color.kamleon_blue_dark),
            focusedContainerColor = colorResource(R.color.color_fa),
            unfocusedContainerColor = colorResource(R.color.color_fa)
        )
    )
}

@Composable
fun AuthPrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    loading: Boolean,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.fillMaxWidth().height(dimensionResource(R.dimen.auth_button_height)).testTag(testTag),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = colorResource(R.color.kamleon_blue_dark),
            contentColor = colorResource(R.color.white)
        )
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.height(24.dp),
                color = colorResource(R.color.white),
                strokeWidth = 2.dp
            )
        } else {
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }
}
