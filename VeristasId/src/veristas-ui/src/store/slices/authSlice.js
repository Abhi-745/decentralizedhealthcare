import { createSlice } from '@reduxjs/toolkit';

const initialState = {
  abhaId: null,
  wallet: null, // Veristas Wallet Details
  role: null, // 'patient', 'paramedic', 'surgeon'
  token: null, // JWT token for current role
  esid: null, // Active emergency session ID if any
};

export const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    loginPatient: (state, action) => {
      state.abhaId = action.payload.abhaId;
      state.wallet = action.payload.wallet;
      state.role = 'patient';
    },
    loginHospitalStaff: (state, action) => {
      state.token = action.payload.token;
      state.role = action.payload.role;
      state.esid = action.payload.esid || null;
    },
    logout: (state) => {
      return initialState;
    },
  },
});

export const { loginPatient, loginHospitalStaff, logout } = authSlice.actions;
export default authSlice.reducer;
