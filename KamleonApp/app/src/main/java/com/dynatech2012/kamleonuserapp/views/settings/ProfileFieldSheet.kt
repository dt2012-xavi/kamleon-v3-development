package com.dynatech2012.kamleonuserapp.views.settings

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.application.AppTheme
import com.dynatech2012.kamleonuserapp.extensions.addYears
import com.dynatech2012.kamleonuserapp.models.CustomUser
import com.dynatech2012.kamleonuserapp.models.Gender
import com.dynatech2012.kamleonuserapp.viewmodels.MainViewModel
import com.dynatech2012.kamleonuserapp.views.KamleonRoundButton
import com.dynatech2012.kamleonuserapp.views.KamleonTextField
import com.dynatech2012.kamleonuserapp.views.kamleonBody
import com.dynatech2012.kamleonuserapp.views.kamleonCaption
import com.dynatech2012.kamleonuserapp.views.kamleonTitle
import com.dynatech2012.kamleonuserapp.views.showActionFailedSheet
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ozcanalasalvar.datepicker.compose.datapicker.WheelDataPicker
import com.ozcanalasalvar.datepicker.compose.datepicker.WheelDatePicker
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import com.ozcanalasalvar.datepicker.model.Date as PickerDate

/** The youngest a user can be: the date of birth cannot be later than this many years ago. */
private const val MIN_AGE_YEARS = 14

/** Rows shown above and below the selected one in the wheels: five in all. */
private const val WHEEL_OFFSET = 2

/**
 * One Personal data field's sheet: a title, the field's control and Save. Save stays off until the
 * value changes (and, for the name, while the name or the surname is empty), turns into a spinner
 * while saving, and [onSave] gets the profile fields to write.
 */
@Composable
fun ProfileFieldSheetContent(
    field: ProfileField,
    user: CustomUser,
    saving: Boolean,
    onSave: (Map<String, Any>) -> Unit
) {
    var pending by remember { mutableStateOf<Map<String, Any>?>(null) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(R.color.white), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(top = 12.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(width = 36.dp, height = 4.dp)
                .background(colorResource(R.color.kamleon_secondary_grey_40), RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(field.title()),
            style = kamleonTitle(),
            color = colorResource(R.color.kamleon_dark_grey),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().testTag("profile_sheet_title")
        )
        Spacer(Modifier.height(20.dp))
        val save = { pending?.let(onSave); Unit }
        when (field) {
            ProfileField.NAME -> NameEditor(user, onChange = { pending = it }, onDone = save)
            ProfileField.GENDER -> GenderEditor(user, onChange = { pending = it })
            ProfileField.BIRTH -> BirthEditor(user, onChange = { pending = it })
            ProfileField.HEIGHT -> HeightEditor(user, onChange = { pending = it })
            ProfileField.WEIGHT -> WeightEditor(user, onChange = { pending = it })
        }
        Spacer(Modifier.height(24.dp))
        KamleonRoundButton(
            text = stringResource(R.string.setting_button_save),
            onClick = save,
            enabled = pending != null,
            loading = saving,
            testTag = "profile_sheet_save"
        )
    }
}

private fun ProfileField.title() = when (this) {
    ProfileField.NAME -> R.string.profile_sheet_title_name
    ProfileField.GENDER -> R.string.profile_sheet_title_gender
    ProfileField.BIRTH -> R.string.profile_sheet_title_birth
    ProfileField.HEIGHT -> R.string.profile_sheet_title_height
    ProfileField.WEIGHT -> R.string.profile_sheet_title_weight
}

/** Name and surname; each must have something besides spaces. Done on the keyboard saves. */
@Composable
private fun NameEditor(user: CustomUser, onChange: (Map<String, Any>?) -> Unit, onDone: () -> Unit) {
    var name by rememberSaveable { mutableStateOf(user.name) }
    var surname by rememberSaveable { mutableStateOf(user.lastName) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    LaunchedEffect(name, surname) {
        val n = name.trim()
        val s = surname.trim()
        val changed = n != user.name.trim() || s != user.lastName.trim()
        onChange(if (n.isNotEmpty() && s.isNotEmpty() && changed) mapOf("name" to n, "lastName" to s) else null)
    }
    Column(Modifier.fillMaxWidth()) {
        FieldLabel(stringResource(R.string.setting_label_name))
        KamleonTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = stringResource(R.string.profile_sheet_name_hint),
            capitalization = KeyboardCapitalization.Words,
            autoCorrect = false,
            focusRequester = focus,
            testTag = "profile_sheet_name"
        )
        Spacer(Modifier.height(16.dp))
        FieldLabel(stringResource(R.string.setting_label_surname))
        KamleonTextField(
            value = surname,
            onValueChange = { surname = it },
            placeholder = stringResource(R.string.profile_sheet_surname_hint),
            capitalization = KeyboardCapitalization.Words,
            autoCorrect = false,
            imeAction = ImeAction.Done,
            onImeAction = onDone,
            testTag = "profile_sheet_surname"
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = kamleonCaption(),
        color = colorResource(R.color.kamleon_secondary_grey_70),
        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
    )
}

/** The four options, the chosen one with a check. */
@Composable
private fun GenderEditor(user: CustomUser, onChange: (Map<String, Any>?) -> Unit) {
    var selected by rememberSaveable { mutableStateOf(user.gender) }
    LaunchedEffect(selected) {
        onChange(if (selected != user.gender) mapOf("gender" to selected.name) else null)
    }
    Column(Modifier.fillMaxWidth().selectableGroup()) {
        Gender.entries.forEach { gender ->
            val isSelected = gender == selected
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .selectable(selected = isSelected, role = Role.RadioButton) { selected = gender }
                    .padding(horizontal = 12.dp)
                    .testTag("profile_sheet_gender_${gender.name}")
            ) {
                Text(
                    text = gender.raw,
                    style = kamleonBody(),
                    color = colorResource(R.color.kamleon_dark_grey)
                )
                if (isSelected) {
                    Image(
                        painter = painterResource(R.drawable.ic_check_24),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(colorResource(R.color.kamleon_blue))
                    )
                }
            }
        }
    }
}

/** The date wheels, never later than [MIN_AGE_YEARS] years ago. */
@Composable
private fun BirthEditor(user: CustomUser, onChange: (Map<String, Any>?) -> Unit) {
    val latest = remember { Date().addYears(-MIN_AGE_YEARS) }
    val start = remember { if (user.dateOfBirth.after(latest)) latest else user.dateOfBirth }
    val saved = remember { user.dateOfBirth.toCalendar() }
    WheelFrame {
        WheelDatePicker(
            offset = WHEEL_OFFSET,
            startDate = PickerDate(start.time),
            maxDate = PickerDate(latest.time),
            yearsRange = IntRange(1900, latest.toCalendar().get(Calendar.YEAR)),
            textSize = 18,
            darkModeEnabled = false,
            onDateChanged = { day, month, year, _ ->
                val changed = day != saved.get(Calendar.DAY_OF_MONTH) ||
                    month != saved.get(Calendar.MONTH) || year != saved.get(Calendar.YEAR)
                onChange(if (changed) mapOf("dateOfBirth" to GregorianCalendar(year, month, day).time) else null)
            }
        )
    }
}

private fun Date.toCalendar(): Calendar = Calendar.getInstance().apply { time = this@toCalendar }

/** Whole centimetres. */
@Composable
private fun HeightEditor(user: CustomUser, onChange: (Map<String, Any>?) -> Unit) {
    val saved = ProfileValues.height(user.height)
    val start = ProfileValues.heightStart(user.height)
    WheelFrame {
        WheelDataPicker(
            offset = WHEEL_OFFSET,
            values = ProfileValues.wheelValues(ProfileValues.HEIGHT_RANGE, start),
            startValue = start.toString(),
            valueUnit = stringResource(R.string.unit_cm),
            showDecimal = false,
            textSize = 20,
            textBold = true,
            valueWidth = 70,
            darkModeEnabled = false,
            onValueChanged = { value, _ ->
                val height = value.toIntOrNull()
                onChange(if (height != null && height != saved) mapOf("height" to height.toFloat()) else null)
            }
        )
    }
}

/** Kilograms with one decimal. */
@Composable
private fun WeightEditor(user: CustomUser, onChange: (Map<String, Any>?) -> Unit) {
    val saved = ProfileValues.weightText(user.weight)?.let { ProfileValues.weightStart(user.weight) }
    val start = ProfileValues.weightStart(user.weight)
    WheelFrame {
        WheelDataPicker(
            offset = WHEEL_OFFSET,
            values = ProfileValues.wheelValues(ProfileValues.WEIGHT_RANGE, start.substringBefore('.').toInt()),
            startValue = start,
            valueUnit = stringResource(R.string.unit_kg),
            showDecimal = true,
            textSize = 20,
            textBold = true,
            valueWidth = 60,
            darkModeEnabled = false,
            onValueChanged = { value, _ ->
                val weight = value.toFloatOrNull()
                onChange(if (weight != null && value != saved) mapOf("weight" to weight) else null)
            }
        )
    }
}

/** The wheels sit on a light rounded card. */
@Composable
private fun WheelFrame(content: @Composable () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colorResource(R.color.kamleon_picker_background))
    ) { content() }
}

/**
 * The sheet for one [ProfileField] of Settings. It saves through [MainViewModel.saveProfile] and
 * waits for the answer itself: on success it closes and tells the screen through
 * [RESULT_SAVED]; on failure it shows the error and stays open so the user can retry.
 */
class ProfileFieldSheet : BottomSheetDialogFragment() {
    private val viewModel: MainViewModel by activityViewModels()
    private val field: ProfileField by lazy {
        ProfileField.valueOf(requireArguments().getString(ARG_FIELD) ?: ProfileField.NAME.name)
    }
    private var saving by mutableStateOf(false)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AppTheme(useDarkTheme = false) {
                    val user by viewModel.userData.observeAsState()
                    // The sheet edits what the profile had when it opened.
                    val opened = remember(user != null) { user }
                    opened?.let { ProfileFieldSheetContent(field, it, saving, ::save) }
                }
            }
        }

    override fun onStart() {
        super.onStart()
        val sheetDialog = dialog as? BottomSheetDialog ?: return
        // The content draws its own rounded surface.
        sheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            ?.setBackgroundColor(Color.TRANSPARENT)
        sheetDialog.behavior.apply {
            skipCollapsed = true
            state = BottomSheetBehavior.STATE_EXPANDED
            // Dragging the wheels must turn them, not pull the sheet down.
            isDraggable = field == ProfileField.NAME || field == ProfileField.GENDER
        }
    }

    private fun save(fields: Map<String, Any>) {
        if (saving) return
        saving = true
        // Leaving mid-save would hide whether it worked.
        isCancelable = false
        viewLifecycleOwner.lifecycleScope.launch {
            val response = viewModel.saveProfile(fields)
            saving = false
            isCancelable = true
            if (response.isSuccess) {
                parentFragmentManager.setFragmentResult(RESULT_SAVED, bundleOf())
                dismissAllowingStateLoss()
            } else {
                showActionFailedSheet(response.error)
            }
        }
    }

    companion object {
        const val RESULT_SAVED = "profile_field_saved"
        private const val ARG_FIELD = "profile_field"

        fun newInstance(field: ProfileField) = ProfileFieldSheet().apply {
            arguments = bundleOf(ARG_FIELD to field.name)
        }
    }
}
